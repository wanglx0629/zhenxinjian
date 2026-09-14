package cn.zhenxinjian.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.zhenxinjian.common.constant.StorageConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import cn.zhenxinjian.domain.vo.FileUploadVO;
import com.aliyun.oss.OSS;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * 对象存储 Service 实现单元测试（需要 mockStatic 处理 Hutool DateUtil/FileUtil/IdUtil）
 * 作者: wanglx
 */
class StorageServiceImplTest {

    private ZhenxinjianProperties zhenxinjianProperties;
    private ObjectProvider<MinioClient> minioClientProvider;
    private ObjectProvider<OSS> ossClientProvider;
    private StorageServiceImpl service;
    private MockedStatic<DateUtil> mockedDateUtil;
    private MockedStatic<FileUtil> mockedFileUtil;
    private MockedStatic<IdUtil> mockedIdUtil;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        zhenxinjianProperties = mock(ZhenxinjianProperties.class);
        minioClientProvider = mock(ObjectProvider.class);
        ossClientProvider = mock(ObjectProvider.class);
        service = new StorageServiceImpl(zhenxinjianProperties, minioClientProvider, ossClientProvider);

        mockedDateUtil = mockStatic(DateUtil.class);
        mockedFileUtil = mockStatic(FileUtil.class);
        mockedIdUtil = mockStatic(IdUtil.class);

        mockedFileUtil.when(() -> FileUtil.extName(anyString())).thenReturn("png");
        mockedIdUtil.when(IdUtil::simpleUUID).thenReturn("test-uuid");
        mockedDateUtil.when(() -> DateUtil.format(any(Date.class), anyString()))
                .thenReturn("2026/09/14");
    }

    @AfterEach
    void tearDown() {
        mockedDateUtil.close();
        mockedFileUtil.close();
        mockedIdUtil.close();
    }

    /** 场景：上传文件为空 → 抛错 */
    @Test
    void upload_nullFile_throwsException() {
        assertThrows(BusinessException.class, () -> service.upload(null));
    }

    /** 场景：文件扩展名不在白名单 → 抛错 */
    @Test
    void upload_invalidExtension_throwsException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.exe");

        ZhenxinjianProperties.Storage storage = storageConfig();
        when(zhenxinjianProperties.getStorage()).thenReturn(storage);

        mockedFileUtil.when(() -> FileUtil.extName("test.exe")).thenReturn("exe");

        assertThrows(BusinessException.class, () -> service.upload(file));
    }

    /** 场景：文件过大 → 抛错 */
    @Test
    void upload_tooLarge_throwsException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("photo.png");
        when(file.getSize()).thenReturn(11L * 1024 * 1024); // 11MB

        ZhenxinjianProperties.Storage storage = storageConfig();
        when(zhenxinjianProperties.getStorage()).thenReturn(storage);

        assertThrows(BusinessException.class, () -> service.upload(file));
    }

    /** 场景：MinIO 和 OSS 均未启用 → 上传失败 */
    @Test
    void upload_noProvider_throwsException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("photo.png");
        when(file.getSize()).thenReturn(100L);

        ZhenxinjianProperties.Storage storage = storageConfig();
        storage.getMinio().setEnabled(false);
        storage.getOss().setEnabled(false);
        when(zhenxinjianProperties.getStorage()).thenReturn(storage);

        assertThrows(BusinessException.class, () -> service.upload(file));
    }

    /** 场景：删除时 objectKey 为空 → 抛错 */
    @Test
    void delete_blankKey_throwsException() {
        assertThrows(BusinessException.class, () -> service.delete(""));
        assertThrows(BusinessException.class, () -> service.delete(null));
    }

    /** 场景：正常上传到 MinIO → 返回 MinIO URL */
    @Test
    void upload_minioEnabled_returnsMinioUrl() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("photo.png");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));

        ZhenxinjianProperties.Storage storage = storageConfig();
        storage.getMinio().setEnabled(true);
        storage.getOss().setEnabled(false);
        when(zhenxinjianProperties.getStorage()).thenReturn(storage);

        MinioClient minioClient = mock(MinioClient.class);
        when(minioClientProvider.getIfAvailable()).thenReturn(minioClient);
        when(minioClient.bucketExists(any())).thenReturn(true);

        FileUploadVO result = service.upload(file);

        assertNotNull(result);
        assertEquals(StorageConstant.PROVIDER_MINIO, result.getProvider());
        assertTrue(result.getUrl().contains("test-uuid"));
    }

    /** 场景：MinIO 上传抛非 BusinessException → 切 OSS 保底 */
    @Test
    void upload_minioFails_fallsBackToOss() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("photo.png");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));

        ZhenxinjianProperties.Storage storage = storageConfig();
        storage.getMinio().setEnabled(true);
        storage.getOss().setEnabled(true);
        when(zhenxinjianProperties.getStorage()).thenReturn(storage);

        // MinIO 可用但 putObject 抛 RuntimeException
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClientProvider.getIfAvailable()).thenReturn(minioClient);
        when(minioClient.bucketExists(any())).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("MinIO error"));
        // OSS 可用
        OSS ossClient = mock(OSS.class);
        when(ossClientProvider.getIfAvailable()).thenReturn(ossClient);

        FileUploadVO result = service.upload(file);

        assertNotNull(result);
        assertEquals(StorageConstant.PROVIDER_OSS, result.getProvider());
    }

    private ZhenxinjianProperties.Storage storageConfig() {
        ZhenxinjianProperties props = new ZhenxinjianProperties();
        ZhenxinjianProperties.Storage storage = new ZhenxinjianProperties.Storage();

        ZhenxinjianProperties.Storage.Minio minio = new ZhenxinjianProperties.Storage.Minio();
        minio.setEnabled(true);
        minio.setEndpoint("http://localhost:9000");
        minio.setBucket("zhenxinjian");
        storage.setMinio(minio);

        ZhenxinjianProperties.Storage.Oss oss = new ZhenxinjianProperties.Storage.Oss();
        oss.setEnabled(true);
        oss.setEndpoint("oss-cn-hangzhou.aliyuncs.com");
        oss.setBucket("zhenxinjian");
        storage.setOss(oss);

        props.setStorage(storage);
        storage.setMaxSizeMb(10);
        storage.setAllowedExtensions(List.of("jpg", "jpeg", "png", "gif", "webp"));
        storage.setPathPrefix("upload/");
        return storage;
    }
}