package me.weishu.kernelsu;

interface IShellService {
    // 执行命令并返回结果
    String exec(String command);
    // 检查某个包名是否已被授权
    boolean checkPermission(String packageName);
    // 请求权限
    void requestPermission(String packageName);
}
