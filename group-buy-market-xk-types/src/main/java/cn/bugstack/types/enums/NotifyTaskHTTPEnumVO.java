package cn.bugstack.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum NotifyTaskHTTPEnumVO {
    SUCCESS("success","成功"),
    ERROR("error","失败"),
    NULL(null,"空操作")
    ;

    String code;
    String info;
}
