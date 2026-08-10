import { load as loadYaml } from 'js-yaml';
import { JSONPath } from 'jsonpath-plus';

export type SpecType = 'asyncapi' | 'openapi';
export type SchemaFormat = 'jsonschema' | 'avro' | 'raw';
export type Selection = {
  message?: string;
  componentMessage?: string;
  channel?: string;
  channelMessage?: string;
  schema?: string;
  operationId?: string;
  operationTarget?: 'request' | 'response';
  statusCode?: string;
  mediaType?: string;
  jsonPath?: string;
};
export type LoadOptions = Selection & {
  url: string;
  specType?: 'auto' | SpecType;
  format?: 'auto' | SchemaFormat;
  headers?: Record<string, string>;
  forwardHeadersTo?: string[];
  env?: Record<string, string | undefined>;
  fetcher?: typeof fetch;
  maxReferenceDepth?: number;
};
export type ResolvedSchema = {
  schema: any;
  format: SchemaFormat;
  specificationType: SpecType;
  specificationUrl: string;
  selector: string;
};

type Document = { data: any; url: string };
type Context = {
  rootUrl: string;
  headers: Record<string, string>;
  allowedOrigins: Set<string>;
  fetcher: typeof fetch;
  maxDepth: number;
};
type Selected = { value: any; selector: string; schemaFormat?: string; sourceDocument: Document };
type NamedCandidate = { key: string; value: any; location: string };

const cache = new Map<string, Promise<Document>>();
const fetchIds = new WeakMap<object, number>();
let nextFetchId = 1;
const methods = new Set(['get', 'put', 'post', 'delete', 'options', 'head', 'patch', 'trace']);
const templatePattern = /\$\{([A-Za-z_][A-Za-z0-9_]*)\}/g;

const isObject = (value: any): value is Record<string, any> => value !== null && typeof value === 'object' && !Array.isArray(value);
const entries = (value: any): [string, any][] => (isObject(value) ? Object.entries(value) : []);
const messageOf = (error: unknown) => (error instanceof Error ? error.message : String(error));
const safeUrl = (value: string) => {
  try {
    const url = new URL(value);
    url.username = '';
    url.password = '';
    return url.toString();
  } catch {
    return value.replace(templatePattern, '${REDACTED}');
  }
};

export function clearRemoteSpecificationSchemaCache() {
  cache.clear();
}

export function resolveTemplateVariables(value: string, env: Record<string, string | undefined> = process.env) {
  return value.replace(templatePattern, (_match, name: string) => {
    if (env[name] === undefined) throw new Error(`Environment variable ${name} is required`);
    return env[name]!;
  });
}

export function parseSpecificationDocument(text: string, url = 'remote document'): any {
  try {
    return JSON.parse(text);
  } catch {
    try {
      const value = loadYaml(text);
      if (!value || typeof value !== 'object') throw new Error('document is not an object');
      return value;
    } catch (error) {
      throw new Error(`Unable to parse ${safeUrl(url)} as JSON or YAML: ${messageOf(error)}`);
    }
  }
}

export function detectSpecificationType(document: any): SpecType {
  if (typeof document?.asyncapi === 'string' && document.asyncapi.startsWith('3.')) return 'asyncapi';
  if (typeof document?.openapi === 'string' && document.openapi.startsWith('3.')) return 'openapi';
  if (document?.asyncapi) throw new Error(`Unsupported AsyncAPI version ${document.asyncapi}; only AsyncAPI 3.x is supported`);
  if (document?.openapi) throw new Error(`Unsupported OpenAPI version ${document.openapi}; only OpenAPI 3.x is supported`);
  throw new Error('Remote document is neither an AsyncAPI 3.x nor an OpenAPI 3.x specification');
}

function fetchIdentity(fetcher: typeof fetch) {
  const key = fetcher as unknown as object;
  if (!fetchIds.has(key)) fetchIds.set(key, nextFetchId++);
  return fetchIds.get(key)!;
}

function cacheKey(url: string, headers: Record<string, string>, fetcher: typeof fetch) {
  const normalized = Object.entries(headers)
    .map(([key, value]) => [key.toLowerCase(), value])
    .sort(([left], [right]) => left.localeCompare(right));
  return `${fetchIdentity(fetcher)}\0${url}\0${JSON.stringify(normalized)}`;
}

function requestHeaders(url: string, context: Context) {
  const origin = new URL(url).origin;
  return origin === new URL(context.rootUrl).origin || context.allowedOrigins.has(origin) ? context.headers : {};
}

async function fetchDocument(url: string, headers: Record<string, string>, context: Context): Promise<Document> {
  const target = new URL(url);
  target.hash = '';
  const canonicalUrl = target.toString();
  const key = cacheKey(canonicalUrl, headers, context.fetcher);
  if (!cache.has(key)) {
    const pending = (async () => {
      let response: Response;
      try {
        response = await context.fetcher(canonicalUrl, { headers });
      } catch (error) {
        throw new Error(`Failed to fetch ${safeUrl(canonicalUrl)}: ${messageOf(error)}`);
      }
      if (!response.ok) throw new Error(`Failed to fetch ${safeUrl(canonicalUrl)}: HTTP ${response.status} ${response.statusText}`.trim());
      return { data: parseSpecificationDocument(await response.text(), canonicalUrl), url: canonicalUrl };
    })();
    cache.set(key, pending);
    pending.catch(() => cache.delete(key));
  }
  return cache.get(key)!;
}

export function resolveJsonPointer(document: any, fragment: string): any {
  if (fragment === '' || fragment === '#') return document;
  const pointer = fragment.startsWith('#') ? fragment.slice(1) : fragment;
  if (!pointer.startsWith('/')) throw new Error(`Unsupported reference fragment ${fragment}`);
  return pointer
    .slice(1)
    .split('/')
    .map((part) => decodeURIComponent(part).replace(/~1/g, '/').replace(/~0/g, '~'))
    .reduce((value, part) => {
      if (value == null || !Object.prototype.hasOwnProperty.call(value, part)) {
        throw new Error(`JSON Pointer ${fragment} does not exist`);
      }
      return value[part];
    }, document);
}

async function resolveValue(
  value: any,
  current: Document,
  context: Context,
  depth = 0,
  stack = new Set<string>()
): Promise<{ value: any; sourceDocument: Document }> {
  if (depth > context.maxDepth) throw new Error(`Maximum reference depth of ${context.maxDepth} exceeded`);
  if (Array.isArray(value)) {
    const values = await Promise.all(value.map((item) => resolveValue(item, current, context, depth, new Set(stack))));
    return { value: values.map((item) => item.value), sourceDocument: current };
  }
  if (!isObject(value)) return { value, sourceDocument: current };
  if (typeof value.$ref === 'string') {
    const refUrl = new URL(value.$ref, current.url);
    const fragment = refUrl.hash;
    refUrl.hash = '';
    const targetUrl = refUrl.toString();
    const identity = `${targetUrl}${fragment}`;
    const siblings = Object.fromEntries(entries(value).filter(([key]) => key !== '$ref'));
    if (stack.has(identity)) return { value: { $ref: value.$ref, ...siblings }, sourceDocument: current };
    const document =
      targetUrl === current.url ? current : await fetchDocument(targetUrl, requestHeaders(targetUrl, context), context);
    const target = fragment ? resolveJsonPointer(document.data, fragment) : document.data;
    const nextStack = new Set(stack).add(identity);
    const resolved = await resolveValue(target, document, context, depth + 1, nextStack);
    const resolvedSiblings = await resolveValue(siblings, current, context, depth, nextStack);
    return {
      value: isObject(resolved.value) ? { ...resolved.value, ...resolvedSiblings.value } : resolved.value,
      sourceDocument: resolved.sourceDocument,
    };
  }
  const children = await Promise.all(
    entries(value).map(async ([key, child]) => [key, (await resolveValue(child, current, context, depth, new Set(stack))).value])
  );
  return { value: Object.fromEntries(children), sourceDocument: current };
}

function available(names: string[]) {
  const sorted = names.sort();
  return `${sorted.slice(0, 12).join(', ')}${sorted.length > 12 ? ', …' : ''}` || '(none)';
}

function smartSelect(values: Record<string, any> | undefined, requested: string, kind: string, fields: string[]) {
  if (!values) throw new Error(`The specification does not define ${kind}`);
  if (Object.prototype.hasOwnProperty.call(values, requested)) return values[requested];
  const matches = entries(values).filter(([, value]) => fields.some((field) => value?.[field] === requested));
  if (matches.length === 1) return matches[0][1];
  if (matches.length > 1) throw new Error(`${kind} selector "${requested}" is ambiguous`);
  throw new Error(`${kind} "${requested}" was not found. Available names: ${available(Object.keys(values))}`);
}

function asyncApiMessageCandidates(document: any): NamedCandidate[] {
  const componentMessages = entries(document?.components?.messages)
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, value]) => ({ key, value, location: `components.messages.${key}` }));
  const channelMessages = entries(document?.channels)
    .sort(([left], [right]) => left.localeCompare(right))
    .flatMap(([channelKey, channel]) =>
      entries(channel?.messages)
        .sort(([left], [right]) => left.localeCompare(right))
        .map(([key, value]) => ({ key, value, location: `channels.${channelKey}.messages.${key}` }))
    );
  return [...componentMessages, ...channelMessages];
}

function selectAsyncApiMessage(document: any, requested: string) {
  const candidates = asyncApiMessageCandidates(document);
  if (!candidates.length) throw new Error('The specification does not define AsyncAPI messages');

  const exactKeyMatches = candidates.filter((candidate) => candidate.key === requested);
  if (exactKeyMatches.length === 1) return exactKeyMatches[0].value;
  if (exactKeyMatches.length > 1) {
    throw new Error(
      `AsyncAPI message key "${requested}" is ambiguous: ${exactKeyMatches.map((candidate) => candidate.location).join(', ')}`
    );
  }

  const metadataMatches = candidates.filter(
    (candidate) => candidate.value?.name === requested || candidate.value?.title === requested
  );
  if (metadataMatches.length === 1) return metadataMatches[0].value;
  if (metadataMatches.length > 1) {
    throw new Error(
      `AsyncAPI message selector "${requested}" is ambiguous: ${metadataMatches
        .map((candidate) => candidate.location)
        .join(', ')}`
    );
  }

  throw new Error(
    `AsyncAPI message "${requested}" was not found. Available names: ${available(candidates.map((candidate) => candidate.key))}`
  );
}

function selectAsyncApiComponentMessage(document: any, requested: string) {
  const messages = document?.components?.messages;
  if (!isObject(messages)) throw new Error('The specification does not define AsyncAPI component messages');
  if (Object.prototype.hasOwnProperty.call(messages, requested)) return messages[requested];
  throw new Error(
    `AsyncAPI component message "${requested}" was not found. Available names: ${available(Object.keys(messages))}`
  );
}

function selectAsyncApiChannelMessage(document: any, channelName: string, requested: string) {
  const channels = document?.channels;
  if (!isObject(channels)) throw new Error('The specification does not define AsyncAPI channels');
  const channel = channels[channelName];
  if (!isObject(channel)) {
    throw new Error(`AsyncAPI channel "${channelName}" was not found. Available names: ${available(Object.keys(channels))}`);
  }
  const messages = channel.messages;
  if (!isObject(messages)) throw new Error(`AsyncAPI channel "${channelName}" does not define messages`);
  if (Object.prototype.hasOwnProperty.call(messages, requested)) return messages[requested];
  throw new Error(
    `AsyncAPI channel message "${requested}" was not found in channel "${channelName}". Available names: ${available(Object.keys(messages))}`
  );
}

function selectionMode(selection: Selection) {
  const modes = [
    selection.message !== undefined && 'message',
    selection.componentMessage !== undefined && 'componentMessage',
    (selection.channel !== undefined || selection.channelMessage !== undefined) && 'channelMessage',
    selection.schema !== undefined && 'schema',
    (selection.operationId !== undefined || selection.operationTarget !== undefined) && 'operation',
    selection.jsonPath !== undefined && 'jsonPath',
  ].filter(Boolean) as string[];
  if (modes.length !== 1) {
    throw new Error(
      'Exactly one selection mode is required: message, componentMessage, channel/channelMessage, schema, operationId/operationTarget, or jsonPath'
    );
  }
  if (modes[0] === 'channelMessage' && (!selection.channel || !selection.channelMessage)) {
    throw new Error('channel and channelMessage must be provided together');
  }
  if (modes[0] === 'operation' && (!selection.operationId || !selection.operationTarget)) {
    throw new Error('operationId and operationTarget must be provided together');
  }
  return modes[0];
}

function findOperation(document: any, operationId: string) {
  const matches = entries(document?.paths).flatMap(([, path]) =>
    entries(path).filter(([method, operation]) => methods.has(method.toLowerCase()) && operation?.operationId === operationId)
  );
  if (!matches.length) throw new Error(`OpenAPI operationId "${operationId}" was not found`);
  if (matches.length > 1) throw new Error(`OpenAPI operationId "${operationId}" is ambiguous`);
  return matches[0][1];
}

function selectMediaType(content: any, requested?: string) {
  const values = entries(content).sort(([left], [right]) => left.localeCompare(right));
  if (!values.length) throw new Error('The selected request or response has no content');
  const selected = requested ? values.find(([name]) => name === requested) : values.find(([name]) => name === 'application/json') ?? values[0];
  if (!selected) throw new Error(`Media type "${requested}" was not found. Available media types: ${available(values.map(([name]) => name))}`);
  if (selected[1]?.schema === undefined) throw new Error(`Media type "${selected[0]}" has no schema`);
  return { name: selected[0], schema: selected[1].schema };
}

function selectResponse(responses: any, status?: string) {
  const values = entries(responses);
  if (!values.length) throw new Error('The selected operation has no responses');
  const selected = status
    ? values.find(([code]) => code === status)
    : values.find(([code]) => code === '200') ??
      values.filter(([code]) => /^2\d\d$/.test(code)).sort(([left], [right]) => left.localeCompare(right))[0] ??
      values.find(([code]) => code === 'default');
  if (!selected) throw new Error(status ? `Response status "${status}" was not found` : 'No 2xx or default response was found');
  return { status: selected[0], response: selected[1] };
}

async function select(document: Document, specType: SpecType, selection: Selection, context: Context): Promise<Selected> {
  const mode = selectionMode(selection);
  if (mode === 'jsonPath') {
    let matches: any[];
    try {
      matches = JSONPath({ path: selection.jsonPath!, json: document.data, wrap: true }) as any[];
    } catch (error) {
      throw new Error(`Invalid JSONPath "${selection.jsonPath}": ${messageOf(error)}`);
    }
    if (!matches.length) throw new Error(`JSONPath "${selection.jsonPath}" matched no values`);
    const allowsMany = /(?:\[\s*\*\s*\]|\.\*)/.test(selection.jsonPath!);
    if (matches.length > 1 && !allowsMany) throw new Error(`JSONPath "${selection.jsonPath}" matched ${matches.length} values`);
    return {
      value: matches.length === 1 ? matches[0] : matches,
      selector: `jsonPath=${selection.jsonPath}`,
      sourceDocument: document,
    };
  }
  if (mode === 'message') {
    if (specType !== 'asyncapi') throw new Error('message requires an AsyncAPI 3.x specification');
    const message = selectAsyncApiMessage(document.data, selection.message!);
    const resolved = await resolveValue(message, document, context);
    const payload = resolved.value?.payload;
    if (payload === undefined) throw new Error(`AsyncAPI message "${selection.message}" has no payload`);
    if (isObject(payload) && typeof payload.schemaFormat === 'string' && payload.schema !== undefined) {
      return {
        value: payload.schema,
        schemaFormat: payload.schemaFormat,
        selector: `message=${selection.message}`,
        sourceDocument: resolved.sourceDocument,
      };
    }
    return { value: payload, selector: `message=${selection.message}`, sourceDocument: resolved.sourceDocument };
  }
  if (mode === 'componentMessage' || mode === 'channelMessage') {
    if (specType !== 'asyncapi') throw new Error(`${mode} requires an AsyncAPI 3.x specification`);
    const message =
      mode === 'componentMessage'
        ? selectAsyncApiComponentMessage(document.data, selection.componentMessage!)
        : selectAsyncApiChannelMessage(document.data, selection.channel!, selection.channelMessage!);
    const resolved = await resolveValue(message, document, context);
    const payload = resolved.value?.payload;
    const label =
      mode === 'componentMessage'
        ? `componentMessage=${selection.componentMessage}`
        : `channel=${selection.channel}, channelMessage=${selection.channelMessage}`;
    if (payload === undefined) throw new Error(`AsyncAPI ${mode} "${mode === 'componentMessage' ? selection.componentMessage : selection.channelMessage}" has no payload`);
    if (isObject(payload) && typeof payload.schemaFormat === 'string' && payload.schema !== undefined) {
      return { value: payload.schema, schemaFormat: payload.schemaFormat, selector: label, sourceDocument: resolved.sourceDocument };
    }
    return { value: payload, selector: label, sourceDocument: resolved.sourceDocument };
  }
  if (mode === 'schema') {
    if (specType !== 'openapi') throw new Error('schema requires an OpenAPI 3.x specification');
    return {
      value: smartSelect(document.data?.components?.schemas, selection.schema!, 'OpenAPI schema', ['title']),
      selector: `schema=${selection.schema}`,
      sourceDocument: document,
    };
  }
  if (specType !== 'openapi') throw new Error('operationId requires an OpenAPI 3.x specification');
  const operation = findOperation(document.data, selection.operationId!);
  if (selection.operationTarget === 'request') {
    if (!operation.requestBody) throw new Error(`OpenAPI operation "${selection.operationId}" has no request body`);
    const body = await resolveValue(operation.requestBody, document, context);
    const media = selectMediaType(body.value.content, selection.mediaType);
    return {
      value: media.schema,
      selector: `operationId=${selection.operationId}, target=request, mediaType=${media.name}`,
      sourceDocument: body.sourceDocument,
    };
  }
  const chosen = selectResponse(operation.responses, selection.statusCode);
  const response = await resolveValue(chosen.response, document, context);
  const media = selectMediaType(response.value.content, selection.mediaType);
  return {
    value: media.schema,
    selector: `operationId=${selection.operationId}, target=response, statusCode=${chosen.status}, mediaType=${media.name}`,
    sourceDocument: response.sourceDocument,
  };
}

export function detectSchemaFormat(schema: any, requested: 'auto' | SchemaFormat = 'auto', schemaFormat?: string, sourceUrl?: string): SchemaFormat {
  if (requested !== 'auto') return requested;
  if (schemaFormat?.toLowerCase().includes('apache.avro')) return 'avro';
  const pathname = sourceUrl ? new URL(sourceUrl).pathname.toLowerCase() : '';
  if (pathname.endsWith('.avsc') || pathname.endsWith('.avro')) return 'avro';
  if (Array.isArray(schema)) return 'avro';
  if (isObject(schema) && ['record', 'enum', 'array', 'map', 'fixed'].includes(schema.type) && (schema.fields || schema.symbols || schema.items || schema.values || schema.size)) return 'avro';
  if (isObject(schema) && ['$schema', 'type', 'properties', 'items', 'allOf', 'oneOf', 'anyOf', '$defs', 'definitions'].some((key) => schema[key] !== undefined)) return 'jsonschema';
  return 'raw';
}

export async function loadRemoteSpecificationSchema(options: LoadOptions): Promise<ResolvedSchema> {
  const env = options.env ?? process.env;
  const url = new URL(resolveTemplateVariables(options.url, env)).toString();
  const headers = Object.fromEntries(entries(options.headers ?? {}).map(([key, value]) => [key, resolveTemplateVariables(value, env)]));
  const allowedOrigins = new Set(
    (options.forwardHeadersTo ?? []).map((value) => {
      try {
        return new URL(value).origin;
      } catch {
        throw new Error(`Invalid forwardHeadersTo origin: ${safeUrl(value)}`);
      }
    })
  );
  const context: Context = {
    rootUrl: url,
    headers,
    allowedOrigins,
    fetcher: options.fetcher ?? fetch,
    maxDepth: options.maxReferenceDepth ?? 25,
  };
  const document = await fetchDocument(url, headers, context);
  const detected = detectSpecificationType(document.data);
  const specType = options.specType && options.specType !== 'auto' ? options.specType : detected;
  if (specType !== detected) throw new Error(`Expected ${specType}, but ${safeUrl(url)} contains ${detected}`);
  let selected = await select(document, specType, options, context);
  if (isObject(selected.value) && selected.value.schema !== undefined && typeof selected.value.schemaFormat === 'string') {
    selected = { ...selected, value: selected.value.schema, schemaFormat: selected.value.schemaFormat };
  }
  const resolved = await resolveValue(selected.value, selected.sourceDocument, context);
  return {
    schema: resolved.value,
    format: detectSchemaFormat(resolved.value, options.format ?? 'auto', selected.schemaFormat, resolved.sourceDocument.url),
    specificationType: specType,
    specificationUrl: safeUrl(url),
    selector: selected.selector,
  };
}

export function sanitizeRemoteSchemaError(error: unknown) {
  return messageOf(error).replace(/(authorization|token|api[-_]?key)(\s*[:=]\s*)\S+/gi, '$1$2[REDACTED]');
}

export function resolveFailOnError(value?: boolean, env = process.env.EVENTCATALOG_REMOTE_SCHEMA_FAIL_ON_ERROR) {
  return value !== undefined ? value : env?.toLowerCase() !== 'false';
}
