(function () {
	let EncrypterValues;

	EncrypterValues = (function () {
        function EncrypterValues(options) {
            this.rve = options.rve, this.C0 = options.C0, this.preC1 = options.preC1;
        }

        return EncrypterValues;

    })();

    module.exports = EncrypterValues;

}).call(this);
