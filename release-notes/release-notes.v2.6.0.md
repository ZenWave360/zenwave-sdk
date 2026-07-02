## What's Changed

Draft release notes for the upcoming **2.6.0** release.

## What's New

### API Overlays

- Fixed object updates for root and wildcard targets so overlay properties are recursively merged without replacing existing content.
- Improved generated AsyncAPI YAML formatting by preserving key order, adding spacing between root sections, omitting the document marker and unnecessary quotes, and placing overlay-added `servers` after `info`.

### CLI

- Fixed handling for missing or mistyped plugin names. The CLI now reports a clear `Plugin not found` error instead of falling through to an empty plugin configuration and failing later with a `NullPointerException` while reading the processor chain.
- Added validation for plugin configurations that do not define a processor chain, so malformed custom plugins fail with a targeted error message.

**Full Changelog**: https://github.com/ZenWave360/zenwave-sdk/compare/v2.5.3...HEAD
