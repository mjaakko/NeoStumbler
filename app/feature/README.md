## feature layer

Feature modules that contain business logic should be placed here.

The structure should be as follows:

```
:feature
    :service - Actual business logic, no Android dependencies
    :infra
        :api - API used to control the feature
        :android - Android implementations, e.g. services, broadcast receivers etc.
```
