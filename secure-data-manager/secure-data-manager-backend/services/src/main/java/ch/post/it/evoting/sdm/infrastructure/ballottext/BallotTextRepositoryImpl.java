/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.ballottext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.record.impl.ODocument;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.domain.model.ballottext.BallotText;
import ch.post.it.evoting.sdm.domain.model.ballottext.BallotTextRepository;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.infrastructure.AbstractEntityRepository;
import ch.post.it.evoting.sdm.infrastructure.DatabaseManager;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;

/**
 * Implementation of the Ballot Repository
 */
@Repository
public class BallotTextRepositoryImpl extends AbstractEntityRepository implements BallotTextRepository {

	/**
	 * Constructor
	 *
	 * @param databaseManager the injected database manager
	 */
	@Autowired
	public BallotTextRepositoryImpl(final DatabaseManager databaseManager) {
		super(databaseManager);
	}

	@PostConstruct
	@Override
	public void initialize() {
		super.initialize();
	}

	@Override
	public void updateSignedBallotText(final String ballotTextId, final String signedBallotText) {
		try {
			ODocument text = getDocument(ballotTextId);
			text.field(JsonConstants.SIGNED_OBJECT, signedBallotText);
			text.field(JsonConstants.STATUS, Status.SIGNED.name());
			saveDocument(text);
		} catch (OException e) {
			throw new DatabaseException("Failed to update signed ballot text.", e);
		}
	}

	@Override
	public List<String> listSignatures(final Map<String, Object> criteria) {
		List<ODocument> documents;
		try {
			documents = listDocuments(criteria);
		} catch (OException e) {
			throw new DatabaseException("Failed to list signatures.", e);
		}
		List<String> signatures = new ArrayList<>(documents.size());
		for (ODocument document : documents) {
			signatures.add(document.field(JsonConstants.SIGNED_OBJECT, String.class));
		}
		return signatures;
	}

	@Override
	protected String entityName() {
		return BallotText.class.getSimpleName();
	}
}
