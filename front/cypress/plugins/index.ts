/**
 * @type {Cypress.PluginConfig}
 */
const registerCodeCoverageTasks = require('@cypress/code-coverage/task');

export default (on: Cypress.PluginEvents, config: Cypress.PluginConfigOptions) => {
  registerCodeCoverageTasks(on, config);
  return config; // important : le plugin attend que tu renvoies config
};
