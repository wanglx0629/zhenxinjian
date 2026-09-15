package cn.zhenxinjian.service;

import cn.zhenxinjian.domain.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 对象存储 Service（MinIO 主存储，OSS 保底）
 * 作者: wanglx
 */
public interface StorageService {

    /** 上传文件 */
    FileUploadVO upload(MultipartFile file);

    /**
     * 按指定 ObjectKey 上传（服务端内部场景，如食物图片启动预热）
     *
     * @param objectKey   对象 Key（如 food/F001.jpg）
     * @param data        文件字节
     * @param contentType MIME 类型
     * @return 上传结果（url/objectKey/provider）
     */
    FileUploadVO upload(String objectKey, byte[] data, String contentType);

    /** 删除文件 */
    void delete(String objectKey);
}
