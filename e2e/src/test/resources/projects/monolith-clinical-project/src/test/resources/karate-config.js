function fn() {
    const port = karate.properties['karate.server.port'] || '8080';
    return {
      baseUrl: 'http://localhost:' + port + '/api',
      auth: {
        username: "user",
        password: "password",
        authMode: "basic",
      },
    };
  }
