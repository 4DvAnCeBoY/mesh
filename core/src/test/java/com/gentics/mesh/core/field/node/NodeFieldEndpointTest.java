package com.gentics.mesh.core.field.node;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.FULL, startServer = true)
public class NodeFieldEndpointTest extends AbstractFieldEndpointTest {

	final String FIELD_NAME = "nodeField";

	@Before
	public void updateSchema() throws Exception {
		try (NoTx noTx = db().noTx()) {
			Schema schema = schemaContainer("folder").getLatestVersion()
					.getSchema();
			NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
			nodeFieldSchema.setName(FIELD_NAME);
			nodeFieldSchema.setLabel("Some label");
			nodeFieldSchema.setAllowedSchemas("folder");
			schema.addField(nodeFieldSchema);
			schemaContainer("folder").getLatestVersion()
					.setSchema(schema);
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		try (NoTx noTx = db().noTx()) {
			Node node = folder("2015");
			List<Node> targetNodes = Arrays.asList(folder("news"), folder("deals"));
			for (int i = 0; i < 20; i++) {
				NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
				Node oldValue = getNodeValue(container, FIELD_NAME);

				Node newValue = targetNodes.get(i % 2);

				// Update the field to point to new target
				NodeResponse response = updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(newValue.getUuid()));
				NodeField field = response.getFields()
						.getNodeField(FIELD_NAME);
				assertThat(field.getUuid()).as("New Value")
						.isEqualTo(newValue.getUuid());
				node.reload();
				container.reload();

				assertEquals("Check version number", container.getVersion()
						.nextDraft()
						.toString(),
						response.getVersion()
								.getNumber());
				assertEquals("Check old value", oldValue, getNodeValue(container, FIELD_NAME));
			}
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (NoTx noTx = db().noTx()) {
			Node target = folder("news");
			NodeResponse firstResponse = updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(target.getUuid()));
			String oldNumber = firstResponse.getVersion()
					.getNumber();

			NodeResponse secondResponse = updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(target.getUuid()));
			assertThat(secondResponse.getVersion()
					.getNumber()).as("New version number")
							.isEqualTo(oldNumber);
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		try (NoTx noTx = db().noTx()) {
			Node target = folder("news");
			NodeResponse firstResponse = updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(target.getUuid()));
			String oldVersion = firstResponse.getVersion()
					.getNumber();

			NodeResponse secondResponse = updateNode(FIELD_NAME, null);
			assertThat(secondResponse.getFields()
					.getNodeField(FIELD_NAME)).as("Deleted Field")
							.isNull();
			assertThat(secondResponse.getVersion()
					.getNumber()).as("New version number")
							.isNotEqualTo(oldVersion);

			// Assert that the old version was not modified
			Node node = folder("2015");
			NodeGraphFieldContainer latest = node.getLatestDraftFieldContainer(english());
			assertThat(latest.getVersion()
					.toString())
							.isEqualTo(secondResponse.getVersion()
									.getNumber());
			assertThat(latest.getNode(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion()
					.getNode(FIELD_NAME)).isNotNull();
			String oldValue = latest.getPreviousVersion()
					.getNode(FIELD_NAME)
					.getNode()
					.getUuid();
			assertThat(oldValue).isEqualTo(target.getUuid());

			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion()
					.getNumber(),
					secondResponse.getVersion()
							.getNumber());
		}
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		try (NoTx noTx = db().noTx()) {
			Node target = folder("news");
			updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(target.getUuid()));
			updateNodeFailure(FIELD_NAME, new NodeFieldImpl(), BAD_REQUEST, "node_error_field_property_missing", "uuid", FIELD_NAME);
		}
	}

	@Test
	public void testUpdateNodeFieldWithNodeJson() {
		try (NoTx noTx = db().noTx()) {
			Node node = folder("news");
			Node node2 = folder("deals");

			Node updatedNode = folder("2015");
			// Load the node so that we can use it to prepare the update request
			NodeResponse loadedNode = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));

			// Update the field to point to node
			NodeResponse response = updateNode(FIELD_NAME, loadedNode.toField());
			NodeField field = response.getFields()
					.getNodeField(FIELD_NAME);
			assertEquals(node.getUuid(), field.getUuid());

			loadedNode = call(() -> client().findNodeByUuid(PROJECT_NAME, updatedNode.getUuid(), new NodeParametersImpl().setLanguages("en"),
					new VersioningParametersImpl().draft()));
			field = loadedNode.getFields()
					.getNodeField(FIELD_NAME);
			assertEquals(node.getUuid(), field.getUuid());

			// Update the field to point to node2
			response = updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(node2.getUuid()));
			field = response.getFields()
					.getNodeField(FIELD_NAME);
			assertEquals(node2.getUuid(), field.getUuid());

			loadedNode = call(() -> client().findNodeByUuid(PROJECT_NAME, updatedNode.getUuid(), new NodeParametersImpl().setLanguages("en"),
					new VersioningParametersImpl().draft()));
			field = loadedNode.getFields()
					.getNodeField("nodeField");
			assertEquals(node2.getUuid(), field.getUuid());
		}
	}

	@Test
	@Ignore("Field deletion is currently not implemented.")
	public void testCreateDeleteNodeField() {
		try (NoTx noTx = db().noTx()) {
			NodeResponse response = createNode(FIELD_NAME, new NodeFieldImpl().setUuid(folder("news").getUuid()));
			NodeField field = response.getFields()
					.getNodeField(FIELD_NAME);
			assertEquals(folder("news").getUuid(), field.getUuid());

			NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
			nodeUpdateRequest.setLanguage("en");
			nodeUpdateRequest.getFields()
					.put(FIELD_NAME, null);

			MeshResponse<NodeResponse> future = client()
					.updateNode(PROJECT_NAME, response.getUuid(), nodeUpdateRequest, new NodeParametersImpl().setLanguages("en"))
					.invoke();
			latchFor(future);
			assertSuccess(future);
			response = future.result();

			assertNull("The field should have been deleted", response.getFields()
					.getNodeField(FIELD_NAME));
		}
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (NoTx noTx = db().noTx()) {
			NodeResponse response = createNode(FIELD_NAME, new NodeFieldImpl().setUuid(folder("news").getUuid()));
			NodeField field = response.getFields()
					.getNodeField(FIELD_NAME);
			assertEquals(folder("news").getUuid(), field.getUuid());
		}
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() throws IOException {
		try (NoTx noTx = db().noTx()) {
			Node newsNode = folder("news");
			Node node = folder("2015");

			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			container.createNode(FIELD_NAME, newsNode);
			NodeResponse response = readNode(node);
			NodeField deserializedNodeField = response.getFields()
					.getNodeField(FIELD_NAME);
			assertNotNull(deserializedNodeField);
			assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());
		}
	}

	@Test
	public void testReadNodeWithResolveLinks() {
		try (NoTx noTx = db().noTx()) {
			Node newsNode = folder("news");
			Node node = folder("2015");

			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			container.createNode(FIELD_NAME, newsNode);

			// Read the node
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en");
			parameters.setResolveLinks(LinkType.FULL);
			NodeResponse response = call(
					() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), parameters, new VersioningParametersImpl().draft()));

			// Check whether the field contains the languagePath
			NodeField deserializedNodeField = response.getFields()
					.getNodeField(FIELD_NAME);
			assertNotNull(deserializedNodeField);
			assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());
			assertNotNull(deserializedNodeField.getPath());
			assertNotNull(deserializedNodeField.getLanguagePaths());
			assertThat(deserializedNodeField.getLanguagePaths()).containsKeys("en", "de");
		}
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		try (NoTx noTx = db().noTx()) {
			NodeResponse response = createNode(FIELD_NAME, (Field) null);
			NodeField field = response.getFields()
					.getNodeField(FIELD_NAME);
			assertNull("The node field within the response should be null since we created the node without providing any field information.", field);
		}
	}

	@Test
	public void testReadNode() throws IOException {
		try (NoTx noTx = db().noTx()) {
			Node referencedNode = folder("news");
			Node node = folder("2015");

			// Create test field
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			container.createNode(FIELD_NAME, referencedNode);

			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));

			// Check node field
			NodeField deserializedNodeField = response.getFields()
					.getNodeField(FIELD_NAME);
			assertNotNull("The referenced field should not be null", deserializedNodeField);

		}
	}

	@Test
	public void testReadNodeNoPerm() throws IOException {
		try (NoTx noTx = db().noTx()) {
			// Revoke the permission to the referenced node
			Node referencedNode = folder("news");
			role().revokePermissions(referencedNode, GraphPermission.READ_PERM);

			Node node = folder("2015");

			// Create test field
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			container.createNode(FIELD_NAME, referencedNode);

			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));

			// Assert that the field has not been loaded
			NodeField deserializedNodeField = response.getFields()
					.getNodeField(FIELD_NAME);
			assertNull("The referenced field should be null", deserializedNodeField);

		}
	}

	@Test
	public void testReadNodeWithExistingField2() throws IOException {
		try (NoTx noTx = db().noTx()) {
			Node newsNode = folder("news");
			Node node = folder("2015");

			// Create test field
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			container.createNode(FIELD_NAME, newsNode);

			// 1. Read node with collapsed fields and check that the collapsed node field can be read
			NodeResponse responseCollapsed = readNode(node);
			NodeField deserializedNodeField = responseCollapsed.getFields()
					.getNodeField(FIELD_NAME);
			assertNotNull(deserializedNodeField);
			assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());

			// Check whether it is possible to read the field.
			deserializedNodeField = responseCollapsed.getFields()
					.getNodeField(FIELD_NAME);
			assertNotNull(deserializedNodeField);

			// Read & check node field
			NodeResponse response = readNode(node);
			deserializedNodeField = response.getFields()
					.getNodeField(FIELD_NAME);
			assertNotNull(deserializedNodeField);
			assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());
		}
	}

	@Test
	public void testReadNodeWithLanguageFallback() {
		try (NoTx noTx = db().noTx()) {
			Node folder = folder("2015");

			// add a node in german and english
			NodeCreateRequest createGermanNode = new NodeCreateRequest();
			createGermanNode.setSchema(new SchemaReference().setName("folder"));
			createGermanNode.setParentNodeUuid(folder.getUuid());
			createGermanNode.setLanguage("de");
			createGermanNode.getFields()
					.put("name", FieldUtil.createStringField("German Target"));

			MeshResponse<NodeResponse> createGermanFuture = client().createNode(PROJECT_NAME, createGermanNode)
					.invoke();
			latchFor(createGermanFuture);
			assertSuccess(createGermanFuture);
			NodeResponse germanTarget = createGermanFuture.result();

			NodeUpdateRequest createEnglishNode = new NodeUpdateRequest();
			createEnglishNode.setLanguage("en");
			createEnglishNode.getFields()
					.put("name", FieldUtil.createStringField("English Target"));

			call(() -> client().updateNode(PROJECT_NAME, germanTarget.getUuid(), createEnglishNode));

			// add a node in german (referencing the target node)
			NodeCreateRequest createSourceNode = new NodeCreateRequest();
			createSourceNode.setSchema(new SchemaReference().setName("folder"));
			createSourceNode.setParentNodeUuid(folder.getUuid());
			createSourceNode.setLanguage("de");
			createSourceNode.getFields()
					.put("name", FieldUtil.createStringField("German Source"));
			createSourceNode.getFields()
					.put(FIELD_NAME, FieldUtil.createNodeField(germanTarget.getUuid()));

			NodeResponse source = call(() -> client().createNode(PROJECT_NAME, createSourceNode));

			// Read source node
			for (String[] requestedLangs : Arrays.asList(new String[] { "de" }, new String[] { "de", "en" }, new String[] { "en", "de" })) {
				NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, source.getUuid(),
						new NodeParametersImpl().setLanguages(requestedLangs), new VersioningParametersImpl().draft()));
				assertEquals("Check node language", "de", response.getLanguage());
				NodeField nodeField = response.getFields()
						.getNodeField(FIELD_NAME);
				assertNotNull("Field must be present", nodeField);
			}
		}
	}

	/**
	 * Get the node value
	 * 
	 * @param container
	 *            container
	 * @param fieldName
	 *            field name
	 * @return node value (may be null)
	 */
	protected Node getNodeValue(NodeGraphFieldContainer container, String fieldName) {
		NodeGraphField field = container.getNode(fieldName);
		return field != null ? field.getNode() : null;
	}
}
