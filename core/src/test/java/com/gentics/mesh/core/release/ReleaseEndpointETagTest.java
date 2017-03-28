package com.gentics.mesh.core.release;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.release.ReleaseListResponse;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.ETag;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class ReleaseEndpointETagTest extends AbstractETagTest {

	@Test
	public void testReadMultiple() {
		try (NoTx noTx = db().noTx()) {
			MeshResponse<ReleaseListResponse> response = client().findReleases(PROJECT_NAME).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(client().findReleases(PROJECT_NAME), etag, true);
			expectNo304(client().findReleases(PROJECT_NAME, new PagingParametersImpl().setPage(2)), etag, true);
		}
	}

	@Test
	public void testReadOne() {
		try (NoTx noTx = db().noTx()) {
			Release release = project().getLatestRelease();
			MeshResponse<ReleaseResponse> response = client().findReleaseByUuid(PROJECT_NAME, release.getUuid()).invoke();
			latchFor(response);
			String etag = release.getETag(mockActionContext());
			assertThat(response.getResponse().getHeader(ETAG)).contains(etag);

			// Check whether 304 is returned for correct etag
			MeshRequest<ReleaseResponse> request = client().findReleaseByUuid(PROJECT_NAME, release.getUuid());
			assertThat(expect304(request, etag, true)).contains(etag);

			// Assert that adding bogus query parameters will not affect the etag
			expect304(client().findReleaseByUuid(PROJECT_NAME, release.getUuid(), new NodeParametersImpl().setLanguages("ru")), etag, true);
		}

	}

}
