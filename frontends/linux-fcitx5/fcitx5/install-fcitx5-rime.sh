#!/usr/bin/env bash
set -euo pipefail

# 自动定位仓库根目录
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$REPO_ROOT"

# 检查并安装 fcitx5-rime
echo "开始安装 fcitx5-rime..."

if [ -f "/etc/os-release" ]; then
    . /etc/os-release
    OS=$ID
    OS_LIKE=${ID_LIKE:-""}
else
    echo "无法找到 /etc/os-release，无法识别 Linux 发行版。"
    echo "请手动安装 fcitx5-rime。"
    exit 1
fi

if [[ "$OS" == "opensuse"* ]] || [[ "$OS_LIKE" == *"suse"* ]]; then
    echo "检测到 openSUSE 系发行版，使用 zypper 安装..."
    sudo zypper install -y fcitx5-rime
elif [[ "$OS" == "fedora" ]] || [[ "$OS_LIKE" == *"fedora"* ]]; then
    echo "检测到 Fedora 系发行版，使用 dnf 安装..."
    sudo dnf install -y fcitx5-rime
elif [[ "$OS" == "arch" ]] || [[ "$OS_LIKE" == *"arch"* ]]; then
    echo "检测到 Arch 系发行版，使用 pacman 安装..."
    sudo pacman -S --noconfirm fcitx5-rime
elif [[ "$OS" == "debian" ]] || [[ "$OS" == "ubuntu" ]] || [[ "$OS_LIKE" == *"debian"* ]]; then
    echo "检测到 Debian/Ubuntu 系发行版，使用 apt 安装..."
    sudo apt update
    sudo apt install -y fcitx5-rime
else
    echo "未知的或不支持的自动安装的发行版 ($OS)。"
    echo "请手动使用您的包管理器安装 fcitx5-rime。"
    exit 1
fi

echo "安装 fcitx5-rime 完成。"
