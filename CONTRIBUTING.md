# Contributing to ZenWave SDK
ZenWave SDK features two complementary contributing models:

- Plugins as a Bazaar
- Core as a Cathedral

## Plugins as a Bazaar

One of the main promises of ZenWave SDK is to provide a plugin system that allows developers to easily extend and personalize the core functionality of the tool, create entirely new plugins or [fork existing ones](https://github.com/ZenWave360/zenwave-sdk#forking-an-standard-or-custom-plugin).

Plugins as packed as standard java jar files and installed on any local, corporate or public maven repo and can be added to jbang or maven classpath as a dependency.

You don't need to ask permission, become a code developer, request write access to this repo or even create pull requests to create your own plugins:

- Just find a plugin that you want to extend or fork
- This [fork command](https://github.com/ZenWave360/zenwave-sdk#forking-an-standard-or-custom-plugin) will download current source code from GitHub, find the plugin you want to fork and will create a new project with the forked source code, performing some basic replacements in java packages names and maven groupId/artifactId
- Do your modifications and install your plugin to any maven repo. Both JBang and Maven will be able to find your plugin as a dependency using your maven settings. You don't even need to publish your changes.
- If you think your changes are worth considering to be merged back to the original plugin. Open an issue following this process, and we will be happy to review your changes and start a discussion:
  - Create a GitHub repo with a base project and use your plugin to generate on top of it.
  - Use different branches or sequential commits to compare generated code from original plugin and your forked plugin.
  - Open an issue with a link to your repo and a description of the changes you made and why you think they are a good addition to the original plugin.

Keep in mind that standard original plugins are designed to be simple, middle of the road, common denominator... and free of over engineering... so they can be useful in the broadest range of use cases. If you need to create a plugin that is too specific for your use case, you can always create and use a custom plugin.

## Core as a Cathedral

Creating and maintaining the core of ZenWave SDK requires time and commitment. If you are inclined to become a core developer consider the following steps as a natural progression:

- Become a zenwave user your self. Use the tool to generate code for your projects and learn how it works.
- Report any issues you find. If you are not sure if it is a bug or a feature request, open an issue, and we will discuss it.
 - If you can, provide any useful pointer about how to fix the issue.
- Propose user-centric new features, in the form of "as a user I want to be able to do X, so that I can achieve Y" or "as a user every time I need to do A, I need to do X,Y and Z, so I would like to be able to do A in a simpler way".
- Provide small pull requests to fix bugs or add new features, so we can get a taste of your coding style and how you think about the problem.

We love your input! We want to make contributing to this project as easy and transparent as possible, but we also want to keep a high bar and make sure that the core of the tool is maintained in a consistent way.

Happy contributing :heart:

## License
When you submit changes, your submissions are understood to be under the same [MIT](https://github.com/ZenWave360/zenwave-sdk/blob/main/LICENSE) that covers the project. Feel free to contact the maintainers if that's a concern.
