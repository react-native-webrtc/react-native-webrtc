/* eslint-disable no-undef */

module.exports = {
    extends: ['eslint:recommended', 'plugin:@typescript-eslint/recommended'],
    parser: '@typescript-eslint/parser',
    plugins: ['@typescript-eslint', 'eslint-plugin-import'],
    root: true,
    overrides: [
        {
            files: ['*.ts', '*.tsx'],
            rules: {
                '@typescript-eslint/ban-ts-comment': 'off',
                '@typescript-eslint/no-empty-interface': 'off',
                '@typescript-eslint/no-explicit-any': 'off',
            },
        },
        {
            files: ['Native*.ts'],
            rules: {
                '@typescript-eslint/ban-types': 'off',
            },
        },
    ],
    rules: {
        'arrow-body-style': ['error', 'as-needed', { requireReturnForObjectLiteral: true }],
        'block-spacing': ['error', 'always'],
        'brace-style': 'error',
        curly: 'error',
        eqeqeq: 'error',
        'import/no-duplicates': 'error',
        'import/order': [
            'error',
            {
                alphabetize: {
                    order: 'asc',
                },
                groups: [['builtin', 'external'], 'parent', 'sibling', 'index'],
                'newlines-between': 'always',
            },
        ],
        'keyword-spacing': 'error',
        'no-mixed-spaces-and-tabs': 'error',
        'no-nested-ternary': 'error',
        'padded-blocks': ['error', 'never'],
        'padding-line-between-statements': [
            'error',
            { blankLine: 'always', prev: ['const', 'let', 'var'], next: '*' },
            { blankLine: 'any', prev: ['const', 'let', 'var'], next: ['const', 'let', 'var'] },
            { blankLine: 'always', prev: '*', next: 'return' },
            { blankLine: 'always', prev: '*', next: 'block-like' },
            { blankLine: 'always', prev: 'block-like', next: '*' },
        ],
        semi: ['error', 'always'],
        'space-before-blocks': 'error',
        'spaced-comment': 'error',
    },
};
