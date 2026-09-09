# LobbyParkour

大厅跑酷存档插件，适配 Paper 1.21.11、EssentialsSpawn 和现有 DoubleJumpZ。

## 行为

- 绿宝石块：记录玩家最后一次经过的精确存档位置，并禁用超级跳。
- 死亡/失误：回大厅 `/spawn`，保留存档，恢复超级跳。
- `/kback`：回到玩家最后记录的存档点，保留存档并禁用超级跳。
- 红石块：放搭路练习风格烟花，回大厅 `/spawn`，清除存档并恢复超级跳。
- 退出或切换子服务器：清除大厅存档；重新进入大厅时恢复超级跳。
- 虚空继续由大厅现有 `VoidSpawnPlus` 处理。

## 构建

```powershell
./gradlew.bat test build
```

产物：`build/libs/LobbyParkour-1.0.0.jar`
