#!/bin/bash
# ==============================================================================
# Farimod Build Target Switcher
# ==============================================================================
# 1 = Website     (downloadSource = 1, درگاه مستقیم وب / زرین‌پال)
# 2 = Cafe Bazaar (downloadSource = 2, کافه بازار)
# 3 = Myket       (downloadSource = 3, پرداخت درون‌برنامه‌ای مایکت)
# ==============================================================================

CONFIG_FILE="shared/src/platform/AppBuildConfig.kt"

get_current() {
    if grep -q "var activeDistribution.*AppDistribution.WEBSITE" "$CONFIG_FILE"; then
        echo "WEBSITE (downloadSource = 1 - درگاه مستقیم وب)"
    elif grep -q "var activeDistribution.*AppDistribution.CAFE_BAZAAR" "$CONFIG_FILE"; then
        echo "CAFE_BAZAAR (downloadSource = 2 - کافه بازار)"
    elif grep -q "var activeDistribution.*AppDistribution.MYKET" "$CONFIG_FILE"; then
        echo "MYKET (downloadSource = 3 - مایکت)"
    else
        echo "UNKNOWN"
    fi
}

set_target() {
    case "$1" in
        website|1)
            sed -i '' 's/var activeDistribution: AppDistribution = .*/var activeDistribution: AppDistribution = AppDistribution.WEBSITE/' "$CONFIG_FILE"
            echo "✅ Target switched to: WEBSITE (downloadSource = 1)"
            echo "   - پرداخت: درگاه مستقیم وب (زرین‌پال)"
            echo "   - لینک بروزرسانی: دانلود با لینک مستقیم"
            ;;
        bazaar|cafebazaar|2)
            sed -i '' 's/var activeDistribution: AppDistribution = .*/var activeDistribution: AppDistribution = AppDistribution.CAFE_BAZAAR/' "$CONFIG_FILE"
            echo "✅ Target switched to: CAFE_BAZAAR (downloadSource = 2)"
            echo "   - پرداخت: آماده برای بازار (فعلاً وب)"
            echo "   - لینک بروزرسانی: کافه بازار"
            ;;
        myket|3)
            sed -i '' 's/var activeDistribution: AppDistribution = .*/var activeDistribution: AppDistribution = AppDistribution.MYKET/' "$CONFIG_FILE"
            echo "✅ Target switched to: MYKET (downloadSource = 3)"
            echo "   - پرداخت: پرداخت درون‌برنامه‌ای مایکت (IAP)"
            echo "   - لینک بروزرسانی: مایکت"
            ;;
        *)
            echo "❌ هدف نامعتبر است! گزینه‌های مجاز: website (1), bazaar (2), myket (3)"
            exit 1
            ;;
    esac
}

case "$1" in
    status|"")
        echo "📌 Current Active Target: $(get_current)"
        echo ""
        echo "راهنما:"
        echo "  ./build-target.sh switch website   # تغییر به خروجی سایت (1)"
        echo "  ./build-target.sh switch bazaar    # تغییر به خروجی کافه بازار (2)"
        echo "  ./build-target.sh switch myket     # تغییر به خروجی مایکت (3)"
        echo "  ./build-target.sh build [target] [debug|release]  # سوئیچ و بیلد پروژه"
        ;;
    switch)
        set_target "$2"
        ;;
    build)
        if [ -n "$2" ]; then
            set_target "$2"
        fi
        VARIANT="${3:-release}"
        echo "🚀 Building androidApp ($VARIANT) for: $(get_current)..."
        ./kotlin task :androidApp:compileAndroid$(tr '[:lower:]' '[:upper:]' <<< ${VARIANT:0:1})${VARIANT:1}
        ;;
    *)
        set_target "$1"
        ;;
esac
