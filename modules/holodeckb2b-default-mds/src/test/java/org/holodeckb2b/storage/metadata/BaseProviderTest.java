package org.holodeckb2b.storage.metadata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.ParameterizedType;

import org.holodeckb2b.interfaces.storage.providers.StorageException;
import org.holodeckb2b.storage.metadata.testhelpers.EntityManagerUtil;
import org.holodeckb2b.storage.metadata.testhelpers.TestMDSProvider;
import org.junit.jupiter.api.BeforeAll;

abstract class BaseProviderTest {
	protected static TestMDSProvider 	provider;

	@BeforeAll
	static void initProvider() throws StorageException {
		provider = new TestMDSProvider();
		provider.init(null);
	}

	protected void assertExistsInDb(MessageUnitEntity<?> entity) {
		Class<?> jpaClass = assertDoesNotThrow(() ->
				Class.forName(((ParameterizedType) entity.getClass().getGenericSuperclass())
																		.getActualTypeArguments()[0].getTypeName()));

		assertNotNull(assertDoesNotThrow(() -> EntityManagerUtil.getEntityManager().find(jpaClass, entity.getOID())));
	}
}
