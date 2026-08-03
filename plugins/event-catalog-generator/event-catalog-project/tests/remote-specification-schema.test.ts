import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  clearRemoteSpecificationSchemaCache,
  detectSpecificationType,
  loadRemoteSpecificationSchema,
  parseSpecificationDocument,
  resolveFailOnError,
  resolveJsonPointer,
  resolveTemplateVariables,
  sanitizeRemoteSchemaError,
} from '../components/lib/remote-specification-schema';

const jsonResponse = (value: unknown, status = 200) =>
  new Response(JSON.stringify(value), { status, statusText: status === 200 ? 'OK' : 'Not Found' });

const asyncApi = (messages: Record<string, any>) => ({
  asyncapi: '3.0.0',
  info: { title: 'Events', version: '1.0.0' },
  components: { messages },
});

const openApi = {
  openapi: '3.1.0',
  info: { title: 'HTTP API', version: '1.0.0' },
  components: {
    schemas: {
      Order: { title: 'Order model', type: 'object', properties: { id: { type: 'string' } } },
      Request: { type: 'object', properties: { value: { type: 'string' } } },
      Result: { type: 'object', properties: { id: { type: 'string' } } },
    },
  },
  paths: {
    '/orders': {
      summary: 'Orders',
      post: {
        operationId: 'createOrder',
        requestBody: {
          content: {
            'text/plain': { schema: { type: 'string' } },
            'application/json': { schema: { $ref: '#/components/schemas/Request' } },
          },
        },
        responses: {
          '201': {
            content: { 'application/json': { schema: { $ref: '#/components/schemas/Result' } } },
          },
          default: {
            content: { 'application/json': { schema: { type: 'string' } } },
          },
        },
      },
    },
  },
};

beforeEach(() => clearRemoteSpecificationSchemaCache());

describe('document parsing and validation', () => {
  it('parses JSON and YAML specifications', () => {
    expect(parseSpecificationDocument('{"openapi":"3.1.0"}')).toEqual({ openapi: '3.1.0' });
    expect(parseSpecificationDocument('asyncapi: 3.0.0\ninfo:\n  title: Events')).toMatchObject({ asyncapi: '3.0.0' });
  });

  it('rejects invalid documents and unsupported versions', () => {
    expect(() => parseSpecificationDocument(':\n  :')).toThrow(/Unable to parse/);
    expect(() => detectSpecificationType({ asyncapi: '2.6.0' })).toThrow(/only AsyncAPI 3.x/);
    expect(() => detectSpecificationType({ title: 'not a specification' })).toThrow(/neither/);
  });

  it('resolves environment templates and errors for missing variables', () => {
    expect(resolveTemplateVariables('Bearer ${TOKEN}', { TOKEN: 'secret' })).toBe('Bearer secret');
    expect(() => resolveTemplateVariables('${MISSING}', {})).toThrow(/MISSING/);
  });
});

describe('AsyncAPI 3 selection', () => {
  it('selects an inline JSON Schema by component key, name, or title', async () => {
    const document = asyncApi({
      Cancelled: {
        name: 'OrderCancelled',
        title: 'Order cancelled',
        payload: { type: 'object', properties: { orderId: { type: 'string' } } },
      },
    });
    const fetcher = vi.fn(async () => jsonResponse(document));

    for (const message of ['Cancelled', 'OrderCancelled', 'Order cancelled']) {
      const result = await loadRemoteSpecificationSchema({ url: 'https://contracts.example/asyncapi.yml', message, fetcher });
      expect(result.format).toBe('jsonschema');
      expect(result.schema.properties.orderId.type).toBe('string');
    }
  });

  it('resolves an internal message reference', async () => {
    const document = asyncApi({
      Base: { payload: { type: 'object', properties: { id: { type: 'string' } } } },
      Alias: { $ref: '#/components/messages/Base' },
    });
    const result = await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/asyncapi.yml',
      message: 'Alias',
      fetcher: vi.fn(async () => jsonResponse(document)),
    });
    expect(result.schema.properties.id.type).toBe('string');
  });

  it('selects an inline channel message with a JSON Schema payload', async () => {
    const document = {
      asyncapi: '3.0.0',
      info: { title: 'Events', version: '1.0.0' },
      channels: {
        'order-events': {
          messages: {
            OrderCancelled: {
              title: 'Order cancelled',
              payload: { type: 'object', properties: { reason: { type: 'string' } } },
            },
          },
        },
      },
    };
    const fetcher = vi.fn(async () => jsonResponse(document));

    const result = await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/asyncapi.yml',
      message: 'OrderCancelled',
      fetcher,
    });

    expect(result.format).toBe('jsonschema');
    expect(result.schema.properties.reason.type).toBe('string');
  });

  it('selects an inline channel message by title and loads its relative Avro payload', async () => {
    const document = {
      asyncapi: '3.0.0',
      info: { title: 'Events', version: '1.0.0' },
      channels: {
        'customer-events': {
          messages: {
            CustomerProfileUpdatedEvent: {
              title: 'Customer profile updated',
              payload: {
                schemaFormat: 'application/vnd.apache.avro+json;version=1.9.0',
                schema: { $ref: './avro/CustomerProfileUpdated.avsc' },
              },
            },
          },
        },
      },
    };
    const fetcher = vi.fn(async (input: string | URL | Request) =>
      String(input).endsWith('.avsc')
        ? jsonResponse({ type: 'record', name: 'CustomerProfileUpdated', fields: [] })
        : jsonResponse(document)
    );

    const result = await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/customer-profile/asyncapi.yml',
      message: 'Customer profile updated',
      fetcher,
    });

    expect(result.format).toBe('avro');
    expect(result.schema.name).toBe('CustomerProfileUpdated');
    expect(fetcher).toHaveBeenCalledWith(
      'https://contracts.example/customer-profile/avro/CustomerProfileUpdated.avsc',
      expect.anything()
    );
  });

  it('rejects duplicate channel message keys deterministically', async () => {
    const document = {
      asyncapi: '3.0.0',
      info: { title: 'Events', version: '1.0.0' },
      channels: {
        alpha: { messages: { Duplicated: { payload: { type: 'string' } } } },
        beta: { messages: { Duplicated: { payload: { type: 'string' } } } },
      },
    };

    await expect(
      loadRemoteSpecificationSchema({
        url: 'https://contracts.example/asyncapi.yml',
        message: 'Duplicated',
        fetcher: vi.fn(async () => jsonResponse(document)),
      })
    ).rejects.toThrow(
      'AsyncAPI message key "Duplicated" is ambiguous: channels.alpha.messages.Duplicated, channels.beta.messages.Duplicated'
    );
  });

  it('selects explicitly located component and channel messages with the same key', async () => {
    const document = {
      asyncapi: '3.0.0',
      info: { title: 'Events', version: '1.0.0' },
      components: {
        messages: {
          FulfillmentFailedMessage: { payload: { type: 'object', properties: { source: { const: 'component' } } } },
        },
      },
      channels: {
        'fulfillment-failed-event-v1': {
          messages: {
            FulfillmentFailedMessage: { payload: { type: 'object', properties: { source: { const: 'channel' } } } },
          },
        },
      },
    };
    const fetcher = vi.fn(async () => jsonResponse(document));

    const component = await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/asyncapi.yml',
      componentMessage: 'FulfillmentFailedMessage',
      fetcher,
    });
    const channel = await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/asyncapi.yml',
      channel: 'fulfillment-failed-event-v1',
      channelMessage: 'FulfillmentFailedMessage',
      fetcher,
    });

    expect(component.schema.properties.source.const).toBe('component');
    expect(channel.schema.properties.source.const).toBe('channel');
  });

  it('requires a channel name together with channelMessage', async () => {
    await expect(
      loadRemoteSpecificationSchema({
        url: 'https://contracts.example/asyncapi.yml',
        channelMessage: 'FulfillmentFailedMessage',
        fetcher: vi.fn(async () => jsonResponse(asyncApi({}))),
      })
    ).rejects.toThrow('channel and channelMessage must be provided together');
  });

  it('unwraps a schema-format payload selected by channelMessage', async () => {
    const document = {
      asyncapi: '3.0.0',
      info: { title: 'Events', version: '1.0.0' },
      channels: {
        orders: {
          messages: {
            OrderCreated: {
              payload: {
                schemaFormat: 'application/vnd.apache.avro+json;version=1.9.0',
                schema: { type: 'record', name: 'OrderCreated', fields: [] },
              },
            },
          },
        },
      },
    };

    const result = await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/asyncapi.yml',
      channel: 'orders',
      channelMessage: 'OrderCreated',
      fetcher: vi.fn(async () => jsonResponse(document)),
    });

    expect(result.format).toBe('avro');
    expect(result.schema.name).toBe('OrderCreated');
  });

  it('loads a relative external Avro payload', async () => {
    const document = asyncApi({
      Cancelled: {
        payload: {
          schemaFormat: 'application/vnd.apache.avro+json;version=1.9.0',
          schema: { $ref: 'avro/OrderCancelled.avsc' },
        },
      },
    });
    const fetcher = vi.fn(async (input: string | URL | Request) => {
      const url = String(input);
      return url.endsWith('.avsc')
        ? jsonResponse({ type: 'record', name: 'OrderCancelled', fields: [{ name: 'id', type: 'string' }] })
        : jsonResponse(document);
    });
    const result = await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/specs/asyncapi.yml',
      message: 'Cancelled',
      fetcher,
    });
    expect(result.format).toBe('avro');
    expect(result.schema.name).toBe('OrderCancelled');
    expect(fetcher).toHaveBeenCalledWith('https://contracts.example/specs/avro/OrderCancelled.avsc', expect.anything());
  });

  it('reports missing and ambiguous smart names', async () => {
    const document = asyncApi({
      One: { name: 'Duplicate', payload: { type: 'string' } },
      Two: { name: 'Duplicate', payload: { type: 'string' } },
    });
    const fetcher = vi.fn(async () => jsonResponse(document));
    await expect(loadRemoteSpecificationSchema({ url: 'https://contracts.example/a.yml', message: 'Missing', fetcher })).rejects.toThrow(
      /Available names: One, Two/
    );
    await expect(loadRemoteSpecificationSchema({ url: 'https://contracts.example/a.yml', message: 'Duplicate', fetcher })).rejects.toThrow(
      /ambiguous/
    );
  });
});

describe('OpenAPI 3 selection', () => {
  it('selects a named component schema', async () => {
    const result = await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/openapi.yml',
      schema: 'Order',
      fetcher: vi.fn(async () => jsonResponse(openApi)),
    });
    expect(result.schema.properties.id.type).toBe('string');
  });

  it('selects operation request and response schemas with deterministic defaults', async () => {
    const fetcher = vi.fn(async () => jsonResponse(openApi));
    const request = await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/openapi.yml',
      operationId: 'createOrder',
      operationTarget: 'request',
      fetcher,
    });
    const response = await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/openapi.yml',
      operationId: 'createOrder',
      operationTarget: 'response',
      fetcher,
    });
    expect(request.schema.properties.value.type).toBe('string');
    expect(response.selector).toContain('statusCode=201');
    expect(response.schema.properties.id.type).toBe('string');
  });

  it('uses an external response document as the base for its relative schema reference', async () => {
    const document: any = structuredClone(openApi);
    document.paths['/orders'].post.responses = {
      '200': { $ref: './responses/create-order.yml' },
    };
    const fetcher = vi.fn(async (input: string | URL | Request) => {
      const url = String(input);
      if (url.endsWith('/responses/create-order.yml')) {
        return jsonResponse({
          description: 'Created',
          content: {
            'application/json': {
              schema: { $ref: '../schemas/CreatedOrder.yml' },
            },
          },
        });
      }
      if (url.endsWith('/schemas/CreatedOrder.yml')) {
        return jsonResponse({ type: 'object', properties: { externalId: { type: 'string' } } });
      }
      return jsonResponse(document);
    });

    const result = await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/api/openapi.yml',
      operationId: 'createOrder',
      operationTarget: 'response',
      fetcher,
    });

    expect(result.schema.properties.externalId.type).toBe('string');
    expect(fetcher).toHaveBeenCalledWith('https://contracts.example/api/schemas/CreatedOrder.yml', expect.anything());
  });

  it('rejects incomplete, missing, and duplicate operation selections', async () => {
    const duplicate = structuredClone(openApi);
    duplicate.paths['/other'] = { post: { ...duplicate.paths['/orders'].post } };
    await expect(
      loadRemoteSpecificationSchema({
        url: 'https://contracts.example/openapi.yml',
        operationId: 'createOrder',
        fetcher: vi.fn(async () => jsonResponse(openApi)),
      })
    ).rejects.toThrow(/provided together/);
    await expect(
      loadRemoteSpecificationSchema({
        url: 'https://contracts.example/openapi.yml',
        operationId: 'missing',
        operationTarget: 'request',
        fetcher: vi.fn(async () => jsonResponse(openApi)),
      })
    ).rejects.toThrow(/was not found/);
    await expect(
      loadRemoteSpecificationSchema({
        url: 'https://contracts.example/openapi.yml',
        operationId: 'createOrder',
        operationTarget: 'request',
        fetcher: vi.fn(async () => jsonResponse(duplicate)),
      })
    ).rejects.toThrow(/ambiguous/);
  });
});

describe('JSONPath and references', () => {
  it('renders the selected JSONPath value rather than the root document', async () => {
    const result = await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/openapi.yml',
      jsonPath: '$.components.schemas.Order',
      fetcher: vi.fn(async () => jsonResponse(openApi)),
    });
    expect(result.schema).toEqual(openApi.components.schemas.Order);
    expect(result.schema.openapi).toBeUndefined();
  });

  it('fails for missing JSONPath results and multiple non-array results', async () => {
    const fetcher = vi.fn(async () => jsonResponse(openApi));
    await expect(
      loadRemoteSpecificationSchema({ url: 'https://contracts.example/openapi.yml', jsonPath: '$.missing', fetcher })
    ).rejects.toThrow(/matched no values/);
    await expect(
      loadRemoteSpecificationSchema({
        url: 'https://contracts.example/openapi.yml',
        jsonPath: '$..properties',
        fetcher,
      })
    ).rejects.toThrow(/matched \d+ values/);
  });

  it('resolves escaped JSON Pointer segments and preserves ref siblings', async () => {
    expect(resolveJsonPointer({ 'a/b': { '~key': 42 } }, '#/a~1b/~0key')).toBe(42);
    const document = {
      ...openApi,
      components: { schemas: { Base: { type: 'object', description: 'base' }, Child: { $ref: '#/components/schemas/Base', title: 'child' } } },
    };
    const result = await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/openapi.yml',
      schema: 'Child',
      fetcher: vi.fn(async () => jsonResponse(document)),
    });
    expect(result.schema).toMatchObject({ type: 'object', description: 'base', title: 'child' });
  });
});

describe('authentication, caching, and failure policy', () => {
  it('forwards headers only to the root origin or an explicit origin', async () => {
    const root = asyncApi({
      Event: {
        payload: { schemaFormat: 'application/vnd.apache.avro+json', schema: { $ref: 'https://schemas.example/Event.avsc' } },
      },
    });
    const calls: Array<{ url: string; headers: HeadersInit | undefined }> = [];
    const fetcher = vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
      calls.push({ url: String(input), headers: init?.headers });
      return String(input).includes('schemas.example')
        ? jsonResponse({ type: 'record', name: 'Event', fields: [] })
        : jsonResponse(root);
    });
    await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/asyncapi.yml',
      message: 'Event',
      headers: { Authorization: 'Bearer ${TOKEN}' },
      env: { TOKEN: 'secret' },
      fetcher,
    });
    expect(calls[0].headers).toEqual({ Authorization: 'Bearer secret' });
    expect(calls[1].headers).toEqual({});

    clearRemoteSpecificationSchemaCache();
    calls.length = 0;
    await loadRemoteSpecificationSchema({
      url: 'https://contracts.example/asyncapi.yml',
      message: 'Event',
      headers: { Authorization: 'Bearer ${TOKEN}' },
      forwardHeadersTo: ['https://schemas.example/path-is-ignored'],
      env: { TOKEN: 'secret' },
      fetcher,
    });
    expect(calls[1].headers).toEqual({ Authorization: 'Bearer secret' });
  });

  it('deduplicates concurrent fetches', async () => {
    const fetcher = vi.fn(async () => jsonResponse(asyncApi({ Event: { payload: { type: 'string' } } })));
    await Promise.all([
      loadRemoteSpecificationSchema({ url: 'https://contracts.example/asyncapi.yml', message: 'Event', fetcher }),
      loadRemoteSpecificationSchema({ url: 'https://contracts.example/asyncapi.yml', message: 'Event', fetcher }),
    ]);
    expect(fetcher).toHaveBeenCalledTimes(1);
  });

  it('uses strict mode by default and sanitizes credential-like errors', () => {
    expect(resolveFailOnError(undefined, undefined)).toBe(true);
    expect(resolveFailOnError(undefined, 'FALSE')).toBe(false);
    expect(resolveFailOnError(true, 'false')).toBe(true);
    expect(sanitizeRemoteSchemaError('Authorization: bearer-secret token=abc')).not.toContain('bearer-secret');
    expect(sanitizeRemoteSchemaError('Authorization: bearer-secret token=abc')).not.toContain('abc');
  });
});
