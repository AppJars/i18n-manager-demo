import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

// This alias is required to make the Vaadin Select addon work with Vaadin 25.
// More info: https://github.com/jouni/theme-select/issues/5
// This configuration is NOT required if your project does not use this addon or runs in Vaadin 24.
const customConfig: UserConfigFn = (_: any) => ({
    resolve: {
        alias: {
            '@vaadin/select/vaadin-lit-select': '@vaadin/select'
        }
    }
});

// overrideVaadinConfig merges the custom config on top of Vaadin's generated config
// (which defines the frontend root, the index.html entry and all Vaadin plugins),
// instead of replacing it.
export default overrideVaadinConfig(customConfig);
