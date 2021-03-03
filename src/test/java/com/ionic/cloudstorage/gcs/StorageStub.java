/*
 * (c) 2019-2021 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.gcs;

import com.google.cloud.Policy;
import com.google.cloud.ReadChannel;
import com.google.cloud.ServiceOptions;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Acl.Entity;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.HmacKey;
import com.google.cloud.storage.PostPolicyV4;
import com.google.cloud.storage.PostPolicyV4.ConditionV4Type;
import com.google.cloud.storage.PostPolicyV4.PostConditionsV4;
import com.google.cloud.storage.PostPolicyV4.PostFieldsV4;
import com.google.cloud.storage.ServiceAccount;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobTargetOption;
import com.google.cloud.storage.Storage.BucketSourceOption;
import com.google.cloud.storage.Storage.BucketTargetOption;
import com.google.cloud.storage.Storage.ComposeRequest;
import com.google.cloud.storage.Storage.CopyRequest;
import com.google.cloud.storage.Storage.SignUrlOption;
import com.google.cloud.storage.StorageBatch;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.WriteChannel;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class StorageStub implements Storage {

    public StorageBatch batch() { return null; }

    public Blob compose(Storage.ComposeRequest composeRequest) { return null; }

    public CopyWriter copy(Storage.CopyRequest copyRequest) { return null; }

    public Blob create(BlobInfo blobInfo, byte[] content, int offset, int length, Storage.BlobTargetOption... options) { return null; }

    public Blob create(BlobInfo blobInfo, byte[] content, Storage.BlobTargetOption... options) { return null; }

    public Blob create(BlobInfo blobInfo, InputStream content, Storage.BlobWriteOption... options) { return null; }

    public Blob create(BlobInfo blobInfo, Storage.BlobTargetOption... options) { return null; }

    public Bucket create(BucketInfo bucketInfo, Storage.BucketTargetOption... options) { return null; }

    public Acl createAcl(BlobId blob, Acl acl) { return null; }

    public Acl createAcl(String bucket, Acl acl) { return null; }

    public Acl createAcl(String bucket, Acl acl, Storage.BucketSourceOption... options) { return null; }

    public Acl createDefaultAcl(String bucket, Acl acl) { return null; }

    public Blob createFrom(BlobInfo blobInfo, Path path, BlobWriteOption... options) throws IOException { return null; }

    public Blob createFrom(BlobInfo blobInfo, Path path, int bufferSize, BlobWriteOption... options) throws IOException { return null; }


    public Blob createFrom(BlobInfo blobInfo, InputStream content, BlobWriteOption... options)throws IOException { return null; }

    public Blob createFrom(BlobInfo blobInfo, InputStream content, int bufferSize, BlobWriteOption... options) throws IOException { return null; }

    public List<Boolean> delete(BlobId... blobIds) { return null; }

    public boolean delete(BlobId blob) { return true; }

    public boolean delete(BlobId blob, Storage.BlobSourceOption... options) { return true; }

    public List<Boolean> delete(Iterable<BlobId> blobIds) { return null; }

    public boolean delete(String bucket, Storage.BucketSourceOption... options) { return true; }

    public boolean delete(String bucket, String blob, Storage.BlobSourceOption... options) { return true; }

    public boolean deleteAcl(BlobId blob, Acl.Entity entity) { return true; }

    public boolean deleteAcl(String bucket, Acl.Entity entity) { return true; }

    public boolean deleteAcl(String bucket, Acl.Entity entity, Storage.BucketSourceOption... options) { return true; }

    public boolean deleteDefaultAcl(String bucket, Acl.Entity entity) { return true; }

    public List<Blob>	get(BlobId... blobIds) { return null; }

    public Blob get(BlobId blob) { return null; }

    public Blob get(BlobId blob, Storage.BlobGetOption... options) { return null; }

    public List<Blob> get(Iterable<BlobId> blobIds) { return null; }

    public Bucket get(String bucket, Storage.BucketGetOption... options) { return null; }

    public Blob get(String bucket, String blob, Storage.BlobGetOption... options) { return null; }

    public Acl getAcl(BlobId blob, Acl.Entity entity) { return null; }

    public Acl getAcl(String bucket, Acl.Entity entity) { return null; }

    public Acl getAcl(String bucket, Acl.Entity entity, Storage.BucketSourceOption... options) { return null; }

    public Acl	getDefaultAcl(String bucket, Acl.Entity entity) { return null; }

    public Policy getIamPolicy(String bucket, Storage.BucketSourceOption... options) { return null; }

    public ServiceAccount getServiceAccount(String projectId) { return null; }

    public com.google.api.gax.paging.Page<Bucket> list(Storage.BucketListOption... options) { return null; }

    public com.google.api.gax.paging.Page<Blob> list(String bucket, Storage.BlobListOption... options) { return null; }

    public List<Acl> listAcls(BlobId blob) { return null; }

    public List<Acl> listAcls(String bucket) { return null; }

    public List<Acl> listAcls(String bucket, Storage.BucketSourceOption... options) { return null; }

    public List<Acl> listDefaultAcls(String bucket) { return null; }

    public Bucket lockRetentionPolicy(BucketInfo bucket, Storage.BucketTargetOption... options) { return null; }

    public byte[] readAllBytes(BlobId blob, Storage.BlobSourceOption... options) { return null; }

    public byte[] readAllBytes(String bucket, String blob, Storage.BlobSourceOption... options) { return null; }

    public ReadChannel reader(BlobId blob, Storage.BlobSourceOption... options) { return null; }

    public ReadChannel reader(String bucket, String blob, Storage.BlobSourceOption... options) { return null; }

    public Policy setIamPolicy(String bucket, Policy policy, Storage.BucketSourceOption... options) { return null; }

    public URL signUrl(BlobInfo blobInfo, long duration, TimeUnit unit, Storage.SignUrlOption... options) { return null; }

    public List<Boolean> testIamPermissions(String bucket, List<String> permissions, Storage.BucketSourceOption... options) { return null; }

    public List<Blob> update(BlobInfo... blobInfos) { return null; }

    public Blob update(BlobInfo blobInfo) { return null; }

    public Blob update(BlobInfo blobInfo, Storage.BlobTargetOption... options) { return null; }

    public Bucket update(BucketInfo bucketInfo, Storage.BucketTargetOption... options) { return null; }

    public List<Blob> update(Iterable<BlobInfo> blobInfos) { return null; }

    public Acl updateAcl(BlobId blob, Acl acl) { return null; }

    public Acl updateAcl(String bucket, Acl acl) { return null; }

    public Acl updateAcl(String bucket, Acl acl, Storage.BucketSourceOption... options) { return null; }

    public Acl updateDefaultAcl(String bucket, Acl acl) { return null; }

    public WriteChannel writer(BlobInfo blobInfo, Storage.BlobWriteOption... options) { return null; }

    public WriteChannel writer(URL signedURL) { return null; }

    public StorageOptions getOptions() { return null; }

    public HmacKey createHmacKey(ServiceAccount serviceAccount, Storage.CreateHmacKeyOption... options) { return null; }

    public void deleteHmacKey(HmacKey.HmacKeyMetadata hmacKeyMetadata, Storage.DeleteHmacKeyOption... options) {}

    public HmacKey.HmacKeyMetadata getHmacKey(String accessId, Storage.GetHmacKeyOption... options) { return null; }

    public com.google.api.gax.paging.Page<HmacKey.HmacKeyMetadata> listHmacKeys( Storage.ListHmacKeysOption... options) { return null; }

    public HmacKey.HmacKeyMetadata	updateHmacKeyState(HmacKey.HmacKeyMetadata hmacKeyMetadata, HmacKey.HmacKeyState state, Storage.UpdateHmacKeyOption... options) { return null; }

    public PostPolicyV4 generateSignedPostPolicyV4(BlobInfo blobInfo, long duration, TimeUnit unit, PostFieldsV4 fields, PostConditionsV4 conditions, PostPolicyV4Option... options) { return null; }

    public PostPolicyV4 generateSignedPostPolicyV4(BlobInfo blobInfo, long duration, TimeUnit unit, PostFieldsV4 fields,PostPolicyV4Option... options) { return null; }

    public PostPolicyV4 generateSignedPostPolicyV4(BlobInfo blobInfo, long duration, TimeUnit unit, PostConditionsV4 conditions, PostPolicyV4Option... options) { return null; }

    public PostPolicyV4 generateSignedPostPolicyV4(BlobInfo blobInfo, long duration, TimeUnit unit, PostPolicyV4Option... options) { return null; }
}
