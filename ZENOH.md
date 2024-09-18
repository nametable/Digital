# Zenoh Extended Digital
Zenoh Extended Digital is Digital with extra Zenoh components for communication over the Zenoh protocol.

## Additional Build Pre-requisites
- Configure `.m2/settings.xml` with credentials to access the Zenoh Maven repository, which is hosted on Github packages (at the time of writing):
    ```xml
    <server>
        <id>github</id>
        <username>your_github_username_here</username>
        <password>your_generated_token_here</password>
    </server>
    ```

## Usage Instructions
### Components
General descriptions of the components in Zenoh Extended Digital can be found within Digital in Help -> Components.

### Rate Limiting
Rate limiting is a feature that can be enabled on the Zenoh Extended Digital components. It is used to limit the rate at which messages are published from components which have the feature enabled. The rate limit can be set in Simulation -> "Circuit specific settings"

## Testing
Documentation for testing Zenoh Extended Digital can be found in the [Zenoh Module Testing](./module_tests/MODULE_TESTS.md) documentation.