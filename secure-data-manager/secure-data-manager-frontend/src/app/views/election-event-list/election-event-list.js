/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
(function () {
    'use strict';

    /*jshint maxparams: 13 */
    angular
        .module('election-event-list', [
            'ui.router',
            'app.sessionService',
            'endpoints',
        ])
        .controller('election-event-list', function (
            $scope,
            $rootScope,
            sessionService,
            endpoints,
            $http,
            $state,
            $mdDialog,
            $q,
            $mdToast,
            toastCustom,
            CustomDialog,
            InProgress,
            gettextCatalog,
            ErrorsDict,
            entitiesCounterService,
        ) {
            const ELECTION_EVENT_MSG_ID = 'Election Event';
            const ELECTION_EVENT_SECURE_MSG_ID = 'Secure election event';
            const ELECTION_EVENT_SECURE_STARTED_MSG_ID = 'Election Event securization started...';
            const ELECTION_EVENT_SECURE_PROGRESS_MSG_ID = 'Securization of this election event is in progress';
            const ELECTION_EVENT_SECURED_MSG_ID = 'Election Event secured';
            const ELECTION_EVENT_SELECT_FIRST_MSG_ID = 'Please select first an Election event with a constituted Administration board';
            const ELECTION_EVENT_WITH_CONSTITUTED_AB_MSG_ID = 'Only election events with a constituted administration board can be secured';
            const ELECTION_EVENT_VIEW_MSG_ID = 'View Election Event(s)';
            const ELECTION_EVENT_VIEW_EXPLANATION_MSG_ID = 'To manage election events you have to secure them first. To do this, select election' +
                ' events from the list and click on the "SECURE" action button above.';
            const EXPORTING_MSG_ID = 'Exporting...';
            const EXPORT_MSG_ID = 'Export';
            const EXPORT_SUCCESS_MSG_ID = 'Data exported successfully';
            const EXPORT_ERROR_MSG_ID = 'Data has not been exported';
            const ERROR_CODE_MSG_ID = 'Error code';
            const CONTACT_SUPPORT_MSG_ID = 'Something went wrong. Contact with Support';

            let alreadySynched = false;

			$scope.data = {};

            $scope.selectEEText = function () {
                return gettextCatalog.getString(ELECTION_EVENT_SELECT_FIRST_MSG_ID);
            };
            $scope.listElectionEvents = function () {
                $scope.errors = {};

                $http.get(endpoints.host() + endpoints.electionEvents).then(
                    function (response) {
                        const data = response.data;
                        $scope.selectedElectionEvent = null;
                        try {
                            sessionService.setElectionEvents(data);
                            $scope.electionEvents = data;
                            $scope.checkAutoSynch();
                        } catch (e) {
                            $scope.data.message = e.message;
                            $scope.errors.electionEventsFailed = true;
                        }
                    },
                    function () {
                        $scope.errors.electionEventsFailed = true;
                    },
                );
            };

            $scope.isABConstituted = function (adminBoardId) {
				let result;
				sessionService.getAdminBoards().forEach(function (adminBoard) {
                    if (adminBoard.id === adminBoardId) {
                        result = adminBoard.status === 'CONSTITUTED';
                    }
                });
                return result;
            };

            /*
                  Lauch the endpoint responsible of generating the configuration of
                  a given election event
                  */
            $scope.generate = function () {
                if ($scope.selectedElectionEvent.status !== 'LOCKED') {
                    new CustomDialog()
                        .title(gettextCatalog.getString(ELECTION_EVENT_SECURE_MSG_ID))
                        .cannotPerform(gettextCatalog.getString(ELECTION_EVENT_MSG_ID))
                        .show();
                    return;
                }

                if (
                    !$scope.isABConstituted(
                        $scope.selectedElectionEvent.administrationAuthority.id,
                    )
                ) {
                    $mdDialog.show(
                        $mdDialog.customAlert({
                            locals: {
                                title: gettextCatalog.getString(ELECTION_EVENT_SECURE_MSG_ID),
                                content: gettextCatalog.getString(ELECTION_EVENT_WITH_CONSTITUTED_AB_MSG_ID),
                            },
                        }),
                    );
                    return;
                }

                $scope.errors = {};

                if ($scope.selectedElectionEvent) {
                    if (!InProgress.contains($scope.selectedElectionEvent.id)) {
                        InProgress.init($scope.selectedElectionEvent.id);
                        try {
							const url = endpoints.electionEvent.replace(
								'{electionEventId}',
								$scope.selectedElectionEvent.id,
							);
							$mdToast.show(
                                toastCustom.topCenter(
                                    gettextCatalog.getString(ELECTION_EVENT_SECURE_STARTED_MSG_ID),
                                    'success',
                                ),
                            );
                            $http.post(endpoints.host() + url).then(
                                function () {
                                    $http.get(endpoints.host() + url).then(function () {
                                        InProgress.finish($scope.selectedElectionEvent.id);
                                        $mdToast.show(
                                            toastCustom.topCenter(
                                                gettextCatalog.getString(ELECTION_EVENT_SECURED_MSG_ID),
                                                'success',
                                            ),
                                        );
                                        $rootScope.$broadcast('refresh-election-events');
                                    });
                                },
                                function (response) {
                                    const data = response.data;
                                    InProgress.finish($scope.selectedElectionEvent.id);
                                    $scope.data.message = JSON.stringify(data);
                                    new CustomDialog()
                                        .title(gettextCatalog.getString(ELECTION_EVENT_SECURE_MSG_ID))
                                        .error()
                                        .show();
                                },
                            );
                        } catch (e) {
                            InProgress.finish($scope.selectedElectionEvent.id);
                            $scope.data.message = e;
                            new CustomDialog()
                                .title(gettextCatalog.getString(ELECTION_EVENT_SECURE_MSG_ID))
                                .error()
                                .show();
                        }
                    } else {
                        $mdDialog.show(
                            $mdDialog.customAlert({
                                locals: {
                                    title: gettextCatalog.getString(ELECTION_EVENT_SECURE_MSG_ID),
                                    content: gettextCatalog.getString(ELECTION_EVENT_SECURE_PROGRESS_MSG_ID),
                                },
                            }),
                        );
                    }
                }
            };

            $scope.goToElectionEvent = function (electionEvent) {
                $scope.errors = {};
                if (electionEvent.status === 'READY') {
                    sessionService.setSelectedElectionEvent(electionEvent);
                    $state.go('ballots');
                } else {
                    $mdDialog.show(
                        $mdDialog.customAlert({
                            locals: {
                                title: gettextCatalog.getString(ELECTION_EVENT_VIEW_MSG_ID),
                                content: gettextCatalog.getString(ELECTION_EVENT_VIEW_EXPLANATION_MSG_ID),
                            },
                        }),
                    );

                }
            };

            $scope.checkAutoSynch = function () {
                // perform autosynch if needed

                if (sessionService.isSync()) {
                    alreadySynched = true;
                }

                if (
                    !alreadySynched &&
                    (!$scope.electionEvents ||
                        !$scope.electionEvents.result ||
                        $scope.electionEvents.result.length === 0)
                ) {
                    alreadySynched = true;
                    $scope.synchronize();
                }
            };

            $scope.uniqueChoice = function (electionEvents, electionEvent) {
                $scope.errors = {};

				let selectedEE = null;

				if (electionEvent.chosen) {
                    electionEvents.forEach(function (o) {
                        if (o.id !== electionEvent.id) {
                            o.chosen = false;
                        } else {
                            selectedEE = o;
                        }
                    });
                }

                if (electionEvent.chosen) {
                    $scope.selectedElectionEvent = selectedEE;
                } else {
                    $scope.selectedElectionEvent = null;
                }

                sessionService.setSelectedElectionEvent(selectedEE);
            };

            $scope.checkSelected = function () {
                return (
                    !$scope.selectedElectionEvent ||
                    $scope.selectedElectionEvent.status !== 'LOCKED'
                );
            };

            $scope.importExportEnabled = function () {
                return sessionService.isImportExportEnabled();
            };

            $scope.getTextIsEESelected = function () {
				const check = $scope.checkSelected();
				return check ? $scope.selectEEText : '';
            };

            $scope.getAdminBoardTitle = function (adminBoardId) {
				let adminBoardTitle = '';
				(sessionService.getAdminBoards() || []).forEach(function (adminBoard) {
                    if (adminBoard.id === adminBoardId) {
                        adminBoardTitle = adminBoard.defaultTitle;
                    }
                });

                return adminBoardTitle;
            };

            //Not used now, but will be used as other US defines
            $scope.isNotReadyToNavigate = function (electionEvent) {
                return electionEvent.status !== 'READY';
            };

            $scope.capitalizeFirstLetter = function (string) {
				const lowerCaseString = string.toLowerCase();
				return (
                    lowerCaseString.charAt(0).toUpperCase() + lowerCaseString.slice(1)
                );
            };

            $scope.checkOneSelected = function () {
                return !$scope.selectedElectionEvent;
            };

            $scope.export = function (path, keyStoreError) {
                $scope.errorInKeystore = keyStoreError;
                $scope.includeElectoralEvent = false;
                $scope.includeVotingCards = false;
                $scope.includeCustomerSpecific = false;
                $scope.password = '';
                $mdDialog
                    .show({
                        controller: function ($scope, $mdDialog) {
                            $scope.exportError = false;
                            $scope.exportCancel = function () {
                                $mdDialog.cancel();
                            };
                            $scope.exportDoExport = function (answer) {
                                $scope.errorInKeystore = false;
                                $scope.exportError =
                                    !$scope.includeElectoralEvent &&
                                    !$scope.includeVotingCards &&
                                    !$scope.includeCustomerSpecific;
                                if (!$scope.exportError) {
                                    $mdDialog.hide(true);
                                }
                            };
                        },
                        templateUrl: 'app/views/dialogs/dialog-export-election-event.html',
                        parent: angular.element(document.body),
                        scope: $scope,
                        clickOutsideToClose: false,
                        escapeToClose: true,
                        preserveScope: true,
                    })
                    .then(function () {
						const url =
							endpoints.host() +
							endpoints.export.replace(
								'{electionEventId}',
								$scope.selectedElectionEvent.id,
							);
						const body = {
							path: path,
							electionEventData: $scope.includeElectoralEvent,
							computedChoiceCodes: $scope.includeElectoralEvent,
							ballotBoxes: $scope.includeElectoralEvent,
							preComputedChoiceCodes: $scope.includeElectoralEvent,
							votingCardsData: $scope.includeVotingCards,
							customerData: $scope.includeCustomerSpecific,
							password: $scope.password,
						};

						$scope.title = gettextCatalog.getString(EXPORTING_MSG_ID);

                        $mdDialog.show({
                            scope: $scope,
                            preserveScope: true,
                            templateUrl: 'app/views/dialogs/dialog-custom-progress-template.html',
                            escapeToClose: false,
                            parent: angular.element(document.body),
                        });

                        $http
                            .post(url, body)
                            .then(function (res) {
                                $mdDialog.hide();
                                $mdToast.show(
                                    toastCustom.topCenter(
                                        gettextCatalog.getString(EXPORT_SUCCESS_MSG_ID),
                                        'success',
                                    ),
                                );
                            })
                            .catch(function (e) {
                                $mdDialog.hide();
                                if (e.data.error == '4005') {
                                    $mdDialog.show(
                                        $mdDialog.customAlert({
                                            locals: {
                                                title: gettextCatalog.getString(EXPORT_ERROR_MSG_ID),
                                                content: gettextCatalog.getString(ErrorsDict(e.data.error)),
                                            },
                                        }),
                                    );
                                } else if (e.data.error == '4009') {
                                    $scope.export(path, true);
                                } else {
                                    $mdToast.show(
                                        toastCustom.topCenter(
                                            gettextCatalog.getString(EXPORT_MSG_ID) +
                                            ': ' +
                                            gettextCatalog.getString(CONTACT_SUPPORT_MSG_ID) +
                                            '. ' +
                                            gettextCatalog.getString(ERROR_CODE_MSG_ID) +
                                            ': ' +
                                            e.data.error +
                                            ', ' +
                                            gettextCatalog.getString(ErrorsDict(e.data.error)),
                                            'error',
                                        ),
                                    );
                                }
                            });
                    });
            };

            $scope.listElectionEvents();

            $scope.$on('refresh-election-events', function () {
                $scope.listElectionEvents();
            });
        });
})();
