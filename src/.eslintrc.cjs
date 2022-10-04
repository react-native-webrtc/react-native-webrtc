/* eslint-disable no-undef */

module.exports = {
    extends: ['eslint:recommended', 'plugin:@typescript-eslint/recommended'],
    parser: '@typescript-eslint/parser',
    plugins: ['@typescript-eslint', 'eslint-plugin-import'],
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
        'arrow-body-style': [
            'error',
            'as-needed',
            { requireReturnForObjectLiteral: true }
        ],
        'array-bracket-spacing': [
            'error',
            'always',
            { 'objectsInArrays': true }
        ],
        'arrow-parens': [ 'error', 'as-needed' ],
        'block-spacing': [ 'error', 'always' ],
        'brace-style': 'error',
        'curly': 'error',
        'eqeqeq': 'error',
        'import/no-duplicates': 'error',
        'import/order': [ 'error', {
            'alphabetize': {
                'order': 'asc'
            },
            'groups': [ [ 'builtin', 'external' ], 'parent', 'sibling', 'index' ],
            'newlines-between': 'always'
        } ],
        'indent': [
            'error',
            4,
            { 'SwitchCase': 1 }
        ],
        'keyword-spacing': 'error',
        'max-len': [ 'error', 120 ],
        'no-mixed-spaces-and-tabs': 'error',
        'no-multiple-empty-lines': 'error',
        'no-nested-ternary': 'error',
        'no-trailing-spaces': 'error',
        'object-curly-spacing': [ 'error', 'always' ],
        'padded-blocks': [ 'error', 'never' ],
        'padding-line-between-statements': [
            'error',
            { blankLine: 'always', prev: [ 'const', 'let', 'var' ], next: '*' },
            { blankLine: 'any', prev: [ 'const', 'let', 'var' ], next: [ 'const', 'let', 'var' ] },
            { blankLine: 'always', prev: '*', next: 'return' },
            { blankLine: 'always', prev: '*', next: 'block-like' },
            { blankLine: 'always', prev: 'block-like', next: '*' },
        ],
        'quotes': [ 'error', 'single' ],
        'semi': [ 'error', 'always' ],
        'space-before-blocks': 'error',
        'spaced-comment': 'error',
    }
};
