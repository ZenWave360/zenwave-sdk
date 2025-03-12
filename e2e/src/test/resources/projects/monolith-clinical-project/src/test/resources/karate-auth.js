// use this function to return a dictionary of authentication/session headers
// you can implement any login workflow including calling other karate features
// take this as an example for Basic authentication: https://github.com/intuit/karate#http-basic-authentication-example
function fn(auth) {
    const credentials = karate.merge(auth || {});
    credentials.authMode = credentials.authMode || 'basic';
    // if empty read password from 'credentials-<env>.yml' files
    credentials.password = credentials.password || karate.get('credentials', {})[credentials.username];
    if (credentials.authMode === 'basic' && credentials.username && credentials.password) {
        const Base64 = Java.type('java.util.Base64');
        const encoded = Base64.getEncoder().encodeToString((credentials.username + ':' + credentials.password).toString().getBytes('utf-8'));
        return {
            Authorization: 'Basic ' + encoded,
        };
    }
    return {
        // Authorization: 'Bearer ...'
    };
}
