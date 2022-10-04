/* eslint-disable no-undef */

module.exports = {
    extends: ['eslint:recommended', 'plugin:@typescript-eslint/recommended'],
    parser: '@typescript-eslint/parser',
    plugins: ['@typescript-eslint'],
    root: true,
    overrides: [
        {
            files: [ '*.ts' ],
            rules: {
                '@typescript-eslint/ban-ts-comment': 'off',
                '@typescript-eslint/no-explicit-any': 'off',
            }
        }
    ],
    rules: {
        'max-len': [ 'error', 120 ],
    }
};
