/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

require('babel-polyfill');

global.angular = require('angular');
require('angular-ui-router');
require('angular-sanitize');
require('angular-gettext');
require('ng-showdown');

require('../../node_modules/angular-ui-bootstrap/src/dropdown/dropdown.js');
require('../../node_modules/angular-ui-bootstrap/src/multiMap/multiMap.js');
require('../../node_modules/angular-ui-bootstrap/src/position/position.js');
