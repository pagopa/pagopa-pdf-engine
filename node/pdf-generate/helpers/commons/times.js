/* use if you want to use a 'for loop' in a template

Example:

{{#each times 4}}
 ...
{{/each}}

* */
function times(n, options) {
  return Array.from({ length: n }, (_, i) => i);
}

module.exports = times;
