### fix 1.7.10 strikepractice plugin arenas reset

### Arena Reset Plugin


ArenaReset is build in spigot 1.7.10
It automatically reads arena locations from StrikePractice and saves all arenas as WorldEdit schematics, making arena resets fast and easy.

### 一、构建插件（Build）
#### 环境要求 / Requirements
- Java 8 (JDK 8)
#### 依赖插件：
- /libs
#### 构建步骤 / Build Steps
```
./gradlew build
```

构建完成后，在 build/libs/ 目录中可以找到

### 二、安装插件（Install）

将生成的 jar 文件放入服务器插件目录：
/plugins/


确保以下插件已安装并正常加载：
- WorldEdit
- StrikePractice

重启服务器

### 三、使用方法（Usage）
1.传送到竞技场/tparena {name}\Teleport to arena

2.使用创世神选区\Use Worldedit to set arena position

3.再一次使用 /tparena {name} 确保你站在loc1\one more time to make sure you standing at loc1.

4.Use //copy

5.Use //schem save {name} 和你的竞技场名字同名 / same name

6.将WorldEdit/schematics中的文件 复制到ArenaReset\schematics \ Copy schematics files to ArenaReset\schematics.

7.done! plugin will auto reset ur Arenas!!

----------------------------------------------------------

#### _自动保存所有竞技场是坏的 Save all arenas automatically (BROKEN)_
_指令 / Command：
/saveallarenas_

### 权限 / Permission：

arenareset.admin


