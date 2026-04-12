#!/data/data/com.termux/files/usr/bin/bash
# =============================================================================
# setup-git-access.sh — One-time setup of git access on Termux
# Run this script ONCE on Termux to configure git clone/pull for GitHub.
# =============================================================================
set -euo pipefail

echo "=== Termux Git Access Setup ==="
echo ""

# ─── 1. Install required packages ────────────────────────────────────────────
echo "[1/5] Installing git, maven, openjdk-21 (if not already installed)..."
pkg update -y -q
pkg install -y git maven openjdk-21

echo ""

# ─── 2. Configure git SSH key ────────────────────────────────────────────────
echo "[2/5] Generating SSH key for GitHub access..."
mkdir -p ~/.ssh
chmod 700 ~/.ssh

KEY_FILE="$HOME/.ssh/github_termux"
if [ -f "$KEY_FILE" ]; then
    echo "  SSH key already exists at $KEY_FILE — skipping generation."
else
    ssh-keygen -t ed25519 -C "termux-deploy" -f "$KEY_FILE" -N ""
    echo "  SSH key generated."
fi

echo ""
echo "  ┌────────────────────────────────────────────────────────────────┐"
echo "  │  Add the following PUBLIC KEY to your GitHub account:          │"
echo "  │  Settings → SSH and GPG keys → New SSH key                     │"
echo "  └────────────────────────────────────────────────────────────────┘"
echo ""
cat "$KEY_FILE.pub"
echo ""
echo "  (Press ENTER after you've added the key to GitHub...)"
read -r

# Configure SSH to use this key for github.com
SSH_CONFIG="$HOME/.ssh/config"
if ! grep -q "Host github.com" "$SSH_CONFIG" 2>/dev/null; then
    cat >> "$SSH_CONFIG" <<EOF

Host github.com
    HostName github.com
    User git
    IdentityFile $KEY_FILE
    IdentitiesOnly yes
EOF
    chmod 600 "$SSH_CONFIG"
    echo "  SSH config updated."
fi

# ─── 3. Test GitHub connection ────────────────────────────────────────────────
echo "[3/5] Testing GitHub SSH connection..."
if ssh -T git@github.com -o StrictHostKeyChecking=no 2>&1 | grep -q "successfully authenticated"; then
    echo "  ✅ GitHub SSH auth OK"
else
    echo "  ⚠️  GitHub responded (this may still be OK if it says 'Hi <username>')"
    ssh -T git@github.com -o StrictHostKeyChecking=no 2>&1 || true
fi

echo ""

# ─── 4. Clone repos ───────────────────────────────────────────────────────────
echo "[4/5] Cloning repos..."
mkdir -p ~/repos

TELEGRAM_BOTS_REPO="git@github.com:YOUR_USERNAME/telegram-bots.git"
TRACE_KEEPER_REPO="git@github.com:YOUR_USERNAME/trace-keeper.git"

echo ""
echo "  ⚠️  Edit this script and replace YOUR_USERNAME with your GitHub username"
echo "  before running step 4. Or clone manually:"
echo ""
echo "    cd ~/repos"
echo "    git clone $TELEGRAM_BOTS_REPO"
echo "    git clone $TRACE_KEEPER_REPO"
echo ""

read -rp "  Enter your GitHub username (or ENTER to skip cloning): " GH_USER
if [ -n "$GH_USER" ]; then
    if [ ! -d "$HOME/repos/telegram-bots/.git" ]; then
        git clone "git@github.com:$GH_USER/telegram-bots.git" ~/repos/telegram-bots
        echo "  ✅ telegram-bots cloned"
    else
        echo "  telegram-bots already cloned — skipping"
    fi

    if [ ! -d "$HOME/repos/trace-keeper/.git" ]; then
        git clone "git@github.com:$GH_USER/trace-keeper.git" ~/repos/trace-keeper
        echo "  ✅ trace-keeper cloned"
    else
        echo "  trace-keeper already cloned — skipping"
    fi
fi

# ─── 5. Create deploy directories ─────────────────────────────────────────────
echo "[5/5] Creating deploy directory structure..."
mkdir -p ~/termuxserver/src
mkdir -p ~/termuxserver/src/sh/logs
echo "  ✅ Directories ready"

echo ""
echo "=== Setup complete! ==="
echo ""
echo "Next steps:"
echo "  1. Build manager-bot locally, copy the jar to Termux:"
echo "     scp -P 8022 manager-bot-1.0-SNAPSHOT-jar-with-dependencies.jar username@192.168.50.89:~/termuxserver/src/"
echo "  2. Copy config.properties for each bot to ~/repos/<repo>/<subpath>/src/main/resources/config.properties"
echo "     (These are secret, never committed to git)"
echo "  3. Launch everything:"
echo "     bash ~/termuxserver/src/sh/run_bot.sh"
echo ""
