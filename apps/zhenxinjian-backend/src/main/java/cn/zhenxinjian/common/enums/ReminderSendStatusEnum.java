package cn.zhenxinjian.common.enums;

/**
 * 提醒推送结果字典枚举（对应 reminder_send_log.send_status 列）
 * 作者: wanglx
 */
public enum ReminderSendStatusEnum {

    /** 成功 */
    SUCCESS(1, "成功"),

    /** 失败 */
    FAIL(2, "失败");

    /** 结果编码（对应 reminder_send_log.send_status 列） */
    private final Integer code;

    /** 结果描述 */
    private final String desc;

    ReminderSendStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按编码查询枚举
     *
     * @param code 结果编码
     * @return 对应枚举；code 无效时返回 null
     */
    public static ReminderSendStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReminderSendStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
