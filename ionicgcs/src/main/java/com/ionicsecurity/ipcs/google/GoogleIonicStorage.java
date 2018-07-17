/*
 * (c) 2017-2018 Ionic Security Inc.
 * By using this code, I agree to the Terms & Conditions (https://dev.ionic.com/use.html)
 * and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.ipcs.google;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.cloud.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Acl.Entity;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageBatch;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;

import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.AgentSdk;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase;
import com.ionic.sdk.error.IonicException;

public class GoogleIonicStorage implements Storage {

    public static final String IONICMETACONSTANT = "ionic-key-id";
    private Storage googleStorage;
    private ISAgentPool agentPool = new ISAgentPool();
    private KeyAttributesMap attributes = null;

    public GoogleIonicStorage(Storage googleStorage) throws IonicException {
        AgentSdk.initialize(null);
        this.googleStorage = googleStorage;
    }

    public GoogleIonicStorage(DeviceProfilePersistorBase persistor, Storage googleStorage) throws IonicException {
        AgentSdk.initialize(null);
        agentPool.setPersistor(persistor);
        this.googleStorage = googleStorage;
    }

    public void setPersistor(DeviceProfilePersistorBase persistor) {
        agentPool.setPersistor(persistor);
    }

    public void setKeyAttributes(KeyAttributesMap attributes) {
        this.attributes = attributes;
    }

    @Override
    public StorageOptions getOptions() {
        return googleStorage.getOptions();
    }

    @SuppressWarnings("deprecation")
    @Override
    public StorageOptions options() {
        return googleStorage.options();
    }

    @Override
    public Bucket create(BucketInfo bucketInfo, BucketTargetOption... options) {
        return googleStorage.create(bucketInfo, options);
    }

    private CreateKeysResponse.Key createIonicKey() throws IonicException {
        Agent agent = agentPool.getAgent();
        CreateKeysResponse keysResponse;
        if (attributes != null) {
            keysResponse = agent.createKey(attributes);
        } else {
            keysResponse = agent.createKey();
        }
        CreateKeysResponse.Key res = keysResponse.getKeys().get(0);
        agentPool.returnAgent(agent);
        return res;
    }

    private GetKeysResponse.Key getIonicKey(String keyid) throws IonicException {
        Agent agent = agentPool.getAgent();
        GetKeysResponse keysResponse = agent.getKey(keyid);
        GetKeysResponse.Key res = keysResponse.getKeys().get(0);
        agentPool.returnAgent(agent);
        return res;
    }

    private <T> ArrayList<T> addOptionToArray(T[] inputArray, T inputOption) {
        ArrayList<T> inputArrList = new ArrayList<T>(Arrays.asList(inputArray));
        inputArrList.add(inputOption);
        return inputArrList;
    }

    private BlobInfo createHelper(BlobInfo blobInfo, CreateKeysResponse.Key ionicKey) {
        HashMap<String, String> blobInfoMetadata;
        if (blobInfo.getMetadata() != null) {
            blobInfoMetadata = new HashMap<String, String>(blobInfo.getMetadata());
        } else {
            blobInfoMetadata = new HashMap<String, String>();
        }

        blobInfoMetadata.put(IONICMETACONSTANT, ionicKey.getId());
        return blobInfo.toBuilder().setMetadata(blobInfoMetadata).build();
    }

    @Override
    public Blob create(BlobInfo blobInfo, BlobTargetOption... options) {
        CreateKeysResponse.Key ionicKey = null;
        try {
            ionicKey = createIonicKey();
        } catch (IonicException e) {
            throw new StorageException(e.getReturnCode(), e.getLocalizedMessage());
        }
        BlobInfo newBlobInfo = createHelper(blobInfo, ionicKey);
        Base64.Encoder encoder = Base64.getEncoder();
        BlobTargetOption[] newOptions;
        BlobTargetOption encBtg = BlobTargetOption.encryptionKey(encoder.encodeToString(ionicKey.getKey()));

        if (options.length == 0 || options[0] == null) {
            newOptions = new BlobTargetOption[1];
            newOptions[0] = encBtg;
        } else {
            newOptions = addOptionToArray(options, encBtg).toArray(new BlobTargetOption[] {});
        }
        return googleStorage.create(newBlobInfo, newOptions);
    }

    @Override
    public Blob create(BlobInfo blobInfo, byte[] content, BlobTargetOption... options) {
        CreateKeysResponse.Key ionicKey = null;
        try {
            ionicKey = createIonicKey();
        } catch (IonicException e) {
            throw new StorageException(e.getReturnCode(), e.getLocalizedMessage());
        }
        BlobInfo newBlobInfo = createHelper(blobInfo, ionicKey);
        Base64.Encoder encoder = Base64.getEncoder();
        BlobTargetOption[] newOptions;
        BlobTargetOption encBtg = BlobTargetOption.encryptionKey(encoder.encodeToString(ionicKey.getKey()));
        if (options.length == 0 || options[0] == null) {
            newOptions = new BlobTargetOption[1];
            newOptions[0] = encBtg;
        } else {
            newOptions = addOptionToArray(options, encBtg).toArray(new BlobTargetOption[] {});
        }

        return googleStorage.create(newBlobInfo, content, newOptions);
    }

    @Override
    public Blob create(BlobInfo blobInfo, InputStream content, BlobWriteOption... options) {
        CreateKeysResponse.Key ionicKey = null;
        try {
            ionicKey = createIonicKey();
        } catch (IonicException e) {
            throw new StorageException(e.getReturnCode(), e.getLocalizedMessage());
        }
        BlobInfo newBlobInfo = createHelper(blobInfo, ionicKey);
        Base64.Encoder encoder = Base64.getEncoder();
        BlobWriteOption[] newOptions;
        BlobWriteOption encBtg = BlobWriteOption.encryptionKey(encoder.encodeToString(ionicKey.getKey()));

        if (options.length == 0 || options[0] == null) {
            newOptions = new BlobWriteOption[1];
            newOptions[0] = encBtg;
        } else {
            newOptions = addOptionToArray(options, encBtg).toArray(new BlobWriteOption[] {});
        }

        return googleStorage.create(newBlobInfo, content, newOptions);
    }

    @Override
    public Bucket get(String bucket, BucketGetOption... options) {
        return googleStorage.get(bucket, options);
    }

    @Override
    public Blob get(String bucket, String blob, BlobGetOption... options) {
        return googleStorage.get(bucket, blob, options);
    }

    @Override
    public Blob get(BlobId blob, BlobGetOption... options) {
        return googleStorage.get(blob, options);
    }

    @Override
    public Blob get(BlobId blob) {
        return googleStorage.get(blob);
    }

    @Override
    public Page<Bucket> list(BucketListOption... options) {
        return googleStorage.list(options);
    }

    @Override
    public Page<Blob> list(String bucket, BlobListOption... options) {
        return googleStorage.list(bucket, options);
    }

    @Override
    public Bucket update(BucketInfo bucketInfo, BucketTargetOption... options) {
        return googleStorage.update(bucketInfo, options);
    }

    @Override
    public Blob update(BlobInfo blobInfo, BlobTargetOption... options) {
        Blob sourceBlob = googleStorage.get(blobInfo.getBlobId());
        String ionicKey = sourceBlob.getMetadata().get(IONICMETACONSTANT);

        HashMap<String, String> blobInfoMetadata = new HashMap<String, String>();
        blobInfoMetadata.putAll(blobInfo.getMetadata());
        blobInfoMetadata.put(IONICMETACONSTANT, ionicKey);
        BlobInfo updatedBlobInfo = BlobInfo.newBuilder(blobInfo.getBlobId()).setMetadata(blobInfoMetadata).build();

        return googleStorage.update(updatedBlobInfo, options);
    }

    @Override
    public Blob update(BlobInfo blobInfo) {
        return this.update(blobInfo, new BlobTargetOption[0]);
    }

    @Override
    public boolean delete(String bucket, BucketSourceOption... options) {
        return googleStorage.delete(bucket, options);
    }

    @Override
    public boolean delete(String bucket, String blob, BlobSourceOption... options) {
        return googleStorage.delete(bucket, blob, options);
    }

    @Override
    public boolean delete(BlobId blob, BlobSourceOption... options) {
        return googleStorage.delete(blob, options);
    }

    @Override
    public boolean delete(BlobId blob) {
        return googleStorage.delete(blob);
    }

    @Override
    public Blob compose(ComposeRequest composeRequest) {
        return googleStorage.compose(composeRequest);
    }

    @Override
    public CopyWriter copy(CopyRequest copyRequest) {
        return googleStorage.copy(copyRequest);
    }

    private BlobSourceOption SourceOptionWithDecKey(Blob blob) {
        Map<String, String> metadata = blob.getMetadata();
        String keyid = metadata.get(IONICMETACONSTANT);

        GetKeysResponse.Key fetchedKey = null;
        try {
            fetchedKey = getIonicKey(keyid);
        } catch (IonicException e) {
            throw new StorageException(e.getReturnCode(), e.getLocalizedMessage());
        }

        Base64.Encoder encoder = Base64.getEncoder();
        return BlobSourceOption.decryptionKey(encoder.encodeToString(fetchedKey.getKey()));
    }

    @Override
    public byte[] readAllBytes(String bucketName, String blobName, BlobSourceOption... options) {
        Blob blob = googleStorage.get(bucketName, blobName, BlobGetOption.fields(BlobField.METADATA));
        BlobSourceOption[] newOptions;
        BlobSourceOption encBsg = SourceOptionWithDecKey(blob);

        if (options.length == 0 || options[0] == null) {
            newOptions = new BlobSourceOption[1];
            newOptions[0] = encBsg;
        } else {
            newOptions = addOptionToArray(options, encBsg).toArray(new BlobSourceOption[] {});
        }

        return googleStorage.readAllBytes(bucketName, blobName, newOptions);
    }

    @Override
    public byte[] readAllBytes(BlobId blobId, BlobSourceOption... options) {
        Blob blob = googleStorage.get(blobId, BlobGetOption.fields(BlobField.METADATA));
        BlobSourceOption[] newOptions;
        BlobSourceOption encBsg = SourceOptionWithDecKey(blob);

        if (options.length == 0 || options.length == 0 || options[0] == null) {
            newOptions = new BlobSourceOption[1];
            newOptions[0] = encBsg;
        } else {
            newOptions = addOptionToArray(options, encBsg).toArray(new BlobSourceOption[] {});
        }

        return googleStorage.readAllBytes(blobId, newOptions);
    }

    @Override
    public StorageBatch batch() {
        return googleStorage.batch();
    }

    @Override
    public ReadChannel reader(String bucket, String blob, BlobSourceOption... options) {
        Blob sourceBlob = googleStorage.get(bucket, blob, BlobGetOption.fields(BlobField.METADATA));
        BlobSourceOption[] newOptions;
        BlobSourceOption decBsg = SourceOptionWithDecKey(sourceBlob);

        if (options.length == 0 || options[0] == null) {
            newOptions = new BlobSourceOption[1];
            newOptions[0] = decBsg;
        } else {
            newOptions = addOptionToArray(options, decBsg).toArray(new BlobSourceOption[] {});
        }

        return googleStorage.reader(bucket, blob, newOptions);
    }

    @Override
    public ReadChannel reader(BlobId blob, BlobSourceOption... options) {
        Blob sourceBlob = googleStorage.get(blob, BlobGetOption.fields(BlobField.METADATA));
        BlobSourceOption[] newOptions;
        BlobSourceOption decBsg = SourceOptionWithDecKey(sourceBlob);

        if (options.length == 0 || options[0] == null) {
            newOptions = new BlobSourceOption[1];
            newOptions[0] = decBsg;
        } else {
            newOptions = addOptionToArray(options, decBsg).toArray(new BlobSourceOption[] {});
        }

        return googleStorage.reader(blob, newOptions);
    }

    @Override
    public WriteChannel writer(BlobInfo blobInfo, BlobWriteOption... options) {
        CreateKeysResponse.Key ionicKey = null;
        try {
            ionicKey = createIonicKey();
        } catch (IonicException e) {
            throw new StorageException(e.getReturnCode(), e.getLocalizedMessage());
        }
        HashMap<String, String> blobInfoMetadata;

        if (blobInfo.getMetadata() != null) {
            blobInfoMetadata = new HashMap<String, String>(blobInfo.getMetadata());
        } else {
            blobInfoMetadata = new HashMap<String, String>();
        }

        blobInfoMetadata.put(IONICMETACONSTANT, ionicKey.getId());

        BlobInfo newBlobInfo = blobInfo.toBuilder().setMetadata(blobInfoMetadata).build();
        Base64.Encoder encoder = Base64.getEncoder();
        BlobWriteOption[] newOptions;
        BlobWriteOption encBwg = BlobWriteOption.encryptionKey(encoder.encodeToString(ionicKey.getKey()));

        if (options.length == 0 || options[0] == null) {
            newOptions = new BlobWriteOption[1];
            newOptions[0] = encBwg;
        } else {
            newOptions = addOptionToArray(options, encBwg).toArray(new BlobWriteOption[] {});
        }

        return googleStorage.writer(newBlobInfo, newOptions);
    }

    @Override
    public URL signUrl(BlobInfo blobInfo, long duration, TimeUnit unit, SignUrlOption... options) {
        return googleStorage.signUrl(blobInfo, duration, unit, options);
    }

    @Override
    public List<Blob> get(BlobId... blobIds) {
        return googleStorage.get(blobIds);
    }

    @Override
    public List<Blob> get(Iterable<BlobId> blobIds) {
        return googleStorage.get(blobIds);
    }

    @Override
    public List<Blob> update(BlobInfo... blobInfos) {
        return googleStorage.update(blobInfos);
    }

    @Override
    public List<Blob> update(Iterable<BlobInfo> blobInfos) {
        return googleStorage.update(blobInfos);
    }

    @Override
    public List<Boolean> delete(BlobId... blobIds) {
        return googleStorage.delete(blobIds);
    }

    @Override
    public List<Boolean> delete(Iterable<BlobId> blobIds) {
        return googleStorage.delete(blobIds);
    }

    @Override
    public Acl getAcl(String bucket, Entity entity) {
        return googleStorage.getAcl(bucket, entity);
    }

    @Override
    public boolean deleteAcl(String bucket, Entity entity) {
        return googleStorage.deleteAcl(bucket, entity);
    }

    @Override
    public Acl createAcl(String bucket, Acl acl) {
        return googleStorage.createAcl(bucket, acl);
    }

    @Override
    public Acl updateAcl(String bucket, Acl acl) {
        return googleStorage.createAcl(bucket, acl);
    }

    @Override
    public List<Acl> listAcls(String bucket) {
        return googleStorage.listAcls(bucket);
    }

    @Override
    public Acl getDefaultAcl(String bucket, Entity entity) {
        return googleStorage.getDefaultAcl(bucket, entity);
    }

    @Override
    public boolean deleteDefaultAcl(String bucket, Entity entity) {
        return googleStorage.deleteDefaultAcl(bucket, entity);
    }

    @Override
    public Acl createDefaultAcl(String bucket, Acl acl) {
        return googleStorage.createDefaultAcl(bucket, acl);
    }

    @Override
    public Acl updateDefaultAcl(String bucket, Acl acl) {
        return googleStorage.updateDefaultAcl(bucket, acl);
    }

    @Override
    public List<Acl> listDefaultAcls(String bucket) {
        return googleStorage.listDefaultAcls(bucket);
    }

    @Override
    public Acl getAcl(BlobId blob, Entity entity) {
        return googleStorage.getAcl(blob, entity);
    }

    @Override
    public boolean deleteAcl(BlobId blob, Entity entity) {
        return googleStorage.deleteAcl(blob, entity);
    }

    @Override
    public Acl createAcl(BlobId blob, Acl acl) {
        return googleStorage.createAcl(blob, acl);
    }

    @Override
    public Acl updateAcl(BlobId blob, Acl acl) {
        return googleStorage.updateAcl(blob, acl);
    }

    @Override
    public List<Acl> listAcls(BlobId blob) {
        return googleStorage.listAcls(blob);
    }

}