#!/bin/bash
# Setor Zero — compilação sem Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner
#
# Precisa do Android SDK em ANDROID_HOME (build-tools e uma plataforma), como no GitHub Actions.
# Assinatura: KEYSTORE (ficheiro, por defeito release.jks), KS_PASS e KS_ALIAS (por defeito "setorzero").
# Se a chave não existir, é criada uma nova (as atualizações por cima têm de usar sempre a mesma chave).
set -e
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Defina ANDROID_HOME com o caminho do Android SDK}"
KEYSTORE=${KEYSTORE:-release.jks}
KS_ALIAS=${KS_ALIAS:-setorzero}
OUT=build/out
rm -rf $OUT && mkdir -p $OUT/cls $OUT/gen $OUT/dex

BT=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)
AJAR=$(ls "$ANDROID_HOME"/platforms/android-*/android.jar | sort -V | tail -1)
echo "SDK: $BT · $AJAR"

"$BT/aapt2" compile --dir res -o $OUT/res.zip
"$BT/aapt2" link -I "$AJAR" --manifest AndroidManifest.xml -o $OUT/base.apk --java $OUT/gen $OUT/res.zip --auto-add-overlay
javac -nowarn -Xlint:-options --release 8 -encoding UTF-8 -cp "$AJAR" -d $OUT/cls $(find src $OUT/gen -name '*.java')
"$BT/d8" --release --min-api 26 --lib "$AJAR" --output $OUT/dex $(find $OUT/cls -name '*.class')
cp $OUT/base.apk $OUT/unaligned.apk
(cd $OUT/dex && zip -q -j ../unaligned.apk classes.dex)
"$BT/zipalign" -f -p 4 $OUT/unaligned.apk $OUT/aligned.apk

if [ ! -f "$KEYSTORE" ]; then
  echo "Aviso: $KEYSTORE não existe — a criar uma chave nova."
  keytool -genkeypair -keystore "$KEYSTORE" -storepass "${KS_PASS:?Defina KS_PASS}" -keypass "$KS_PASS" -alias "$KS_ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Setor Zero, C=PT"
fi
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-pass "pass:${KS_PASS:?Defina KS_PASS}" --ks-key-alias "$KS_ALIAS" --out build/SetorZero.apk $OUT/aligned.apk
"$BT/apksigner" verify build/SetorZero.apk && ls -la build/SetorZero.apk
