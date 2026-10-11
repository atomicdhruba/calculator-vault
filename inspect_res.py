from androguard.core.apk import APK

apk = APK('Calculatorcom.example.calculatorv1.0.apk')
resources = apk.get_android_resources()
packages = resources.get_packages_names()

print("Resource packages:", packages)
for pkg in packages:
    strings = resources.get_strings_resources()
    print(f"Total string resources: {len(strings)}")
    for name, val in list(strings.items())[:30]:
        print(f"  {name} = {val}")
