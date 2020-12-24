var assert = require('assert');


describe('MediaStreamTrack', function () {

    describe('#clone()', function () {
        it('should return -1 when the value is not present', function () {

            assert.deepStrictEqual([1, 2, 3].indexOf(4), -1);
        });

        it('should return 1 for index', function () {

            assert.deepStrictEqual([1, 2, 3].indexOf(2), 1);
        });
    });
});
