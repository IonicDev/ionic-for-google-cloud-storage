/*
 * (c) 2017-2018 Ionic Security Inc. By using this code, I agree to the Terms & Conditions
 * (https://dev.ionic.com/use) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.ipcs.google;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.FixMethodOrder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Storage.BlobSourceOption;
import com.google.cloud.storage.Storage.BlobTargetOption;
import com.google.cloud.storage.Storage.BlobWriteOption;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.agent.request.getkey.GetKeysRequest;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.error.IonicException;
import static com.ionicsecurity.ipcs.google.GoogleIonicStorage.IONICMETACONSTANT;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GoogleIonicStorageUnitTest {
  private static Agent ionicAgent;
  private static ISAgentPool ionicAgentPool;
  private static DeviceProfilePersistorPlainText ptPersistor;
  private static CreateKeysResponse.Key ionicKeyCreated;
  private static GetKeysResponse.Key ionicKeyFetched;
  private static Storage googleStorage;
  private static String bucketName = System.getenv("IPCS_GCS_TEST_BUCKET");
  private static String blobName = "test";
  private static String IonicKeyID = "nokey";
  private static String blobContent;

  private static Base64.Encoder encoder;
  private static BlobTargetOption[] BTO;
  private static BlobWriteOption[] BWO;
  private static BlobInfo blobInfoArg;
  private static BlobId blobId;
  private static Map<String, String> blobInfoMetadata;
  private static BlobSourceOption[] BSO;
  private static BlobInfo bliWithMeta;
  private static byte[] blobBytes;
  private static ReadChannel readChannel;
  private static WriteChannel writeChannel;

  @BeforeClass
  public static void init() throws InvalidPathException, IOException, IonicException {
    googleStorage = StorageOptions.getDefaultInstance().getService();
    blobInfoArg = BlobInfo.newBuilder(BlobId.of(bucketName, blobName)).build();

    blobId = BlobId.of(bucketName, blobName);
    blobContent = "This is the functional test blob";
    blobBytes = blobContent.getBytes();
    encoder = Base64.getEncoder();

    ptPersistor = new DeviceProfilePersistorPlainText();
    String sProfilePath = Paths.get(System.getProperty("user.home") + "/.ionicsecurity/profiles.pt")
        .toFile().getCanonicalPath();
    ptPersistor.setFilePath(sProfilePath);

    ionicAgentPool = new ISAgentPool();
    ionicAgentPool.setPersistor(ptPersistor);
  }

  @Before
  public void initEach() throws IonicException {
    ionicAgent = ionicAgentPool.getAgent();
    googleStorage = StorageOptions.getDefaultInstance().getService();
  }

  @After
  public void releEach() {
    ionicAgentPool.returnAgent(ionicAgent);
  }

  @Test
  public void test01_CreateKey() throws Exception {
    CreateKeysRequest request = new CreateKeysRequest();
    CreateKeysRequest.Key requestKey = new CreateKeysRequest.Key("ref", 1);
    request.getKeys().add(requestKey);
    CreateKeysResponse keysResponse;
    keysResponse = ionicAgent.createKeys(request);
    assertNotNull(ionicKeyCreated = keysResponse.getKeys().get(0));
  }

  @Test
  public void test02_FetchKey() throws Exception {
    GetKeysRequest request = new GetKeysRequest();
    request.getKeyIds().add(ionicKeyCreated.getId());
    GetKeysResponse response = ionicAgent.getKeys(request);
    assertNotNull(ionicKeyFetched = response.getKeys().get(0));
  }

  @Test
  public void test03_CreateWithTargetOptions() {
    blobInfoMetadata = new HashMap<String, String>();
    blobInfoMetadata.put(IONICMETACONSTANT, ionicKeyCreated.getId());
    bliWithMeta = blobInfoArg.toBuilder().setMetadata(blobInfoMetadata).build();

    BlobTargetOption encBtg =
        BlobTargetOption.encryptionKey(encoder.encodeToString(ionicKeyCreated.getKey()));
    BTO = new BlobTargetOption[1];
    BTO[0] = encBtg;

    assertNotNull(googleStorage.create(bliWithMeta, blobBytes, BTO));
  }

  @Test
  public void test04_Reader() throws Exception {
    BlobSourceOption encBso =
        BlobSourceOption.decryptionKey(encoder.encodeToString(ionicKeyFetched.getKey()));
    BSO = new BlobSourceOption[1];
    BSO[0] = encBso;

    ByteBuffer dlContent = ByteBuffer.allocate(blobBytes.length);
    ByteBuffer orgBlobBytes = ByteBuffer.wrap(blobBytes);

    assertNotNull(readChannel = googleStorage.reader(bucketName, blobName, BSO));
    readChannel.read(dlContent);
    dlContent.position(0);
    assertEquals(orgBlobBytes.compareTo(dlContent), 0);

    assertNotNull(readChannel = googleStorage.reader(blobId, BSO));
    readChannel.read(dlContent);
    dlContent.position(0);
    assertEquals(orgBlobBytes.compareTo(dlContent), 0);
  }

  @Test
  public void test05_Writer() throws Exception {
    BlobWriteOption encBwo =
        BlobWriteOption.encryptionKey(encoder.encodeToString(ionicKeyCreated.getKey()));
    BWO = new BlobWriteOption[1];
    BWO[0] = encBwo;

    assertNotNull(writeChannel = googleStorage.writer(blobInfoArg, BWO));
    try {
      writeChannel.write(ByteBuffer.wrap(blobBytes, 0, blobBytes.length));
    } finally {
      writeChannel.close();
    }
  }

  @Test
  public void test06_ReadAllBytes() {
    assertArrayEquals(googleStorage.readAllBytes(bucketName, blobName, BSO), blobBytes);
  }

  @Test
  public void test07_Update() {
    blobInfoMetadata = new HashMap<String, String>();
    blobInfoMetadata.put(IONICMETACONSTANT, IonicKeyID);
    bliWithMeta = blobInfoArg.toBuilder().setMetadata(blobInfoMetadata).build();

    assertNotNull(googleStorage.update(bliWithMeta, BTO));
    assertEquals(IonicKeyID.compareTo(blobInfoMetadata.get(IONICMETACONSTANT)), 0);
  }

}
