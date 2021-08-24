(function () {
	let EncryptionParams, elGamalService, forge, mathematicalService;

	mathematicalService = (require('cryptolib-js/src/mathematical')).newService();

	elGamalService = (require('cryptolib-js/src/elgamal')).newService();

	forge = require('node-forge');

	EncryptionParams = (function () {
		function EncryptionParams(options) {
			this.serializedP = options.serializedP;
			this.serializedQ = options.serializedQ;
			this.serializedG = options.serializedG;
			this.serializedOptionsEncryptionKey = options.serializedOptionsEncryptionKey;
			this.serializedChoiceCodesEncryptionKey = options.serializedChoiceCodesEncryptionKey;
			this.p = new forge.jsbn.BigInteger(this.serializedP);
			this.q = new forge.jsbn.BigInteger(this.serializedQ);
			this.g = new forge.jsbn.BigInteger(this.serializedG);
			this.group = mathematicalService.newZpSubgroup(this.p, this.q, this.g);
			this.optionsEncryptionKey = elGamalService.newPublicKey(this.serializedOptionsEncryptionKey);
			this.choiceCodesEncryptionKey = elGamalService.newPublicKey(this.serializedChoiceCodesEncryptionKey);
		}

		return EncryptionParams;

	})();

	module.exports = EncryptionParams;

}).call(this);
