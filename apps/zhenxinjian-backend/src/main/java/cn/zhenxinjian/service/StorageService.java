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

    /** 删除文件 */
    void delete(String objectKey);
}
