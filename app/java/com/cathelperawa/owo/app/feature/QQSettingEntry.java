package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.SettingsInjectQQ;

@HookItem(name = "QQ设置入口", path = "移植/QQ", order = 1,
        description = "在 QQ 设置页插入模块入口（与 TIM 逻辑分开）",
        defaultEnabled = true, hosts = {"qq"})
public class QQSettingEntry extends HookItemBase {

    public static final String K = "qq_setting_entry";

    @Override
    public String switchKey() {
        return K;
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("打开 QQ 设置页即可看到入口");
        try {
            SettingsInjectQQ.install(cl);
            hit();
        } catch (Throwable t) {
            fail("QQ 设置入口注入失败", t);
        }
    }
}
