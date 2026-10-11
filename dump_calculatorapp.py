from loguru import logger
logger.remove()
from androguard.core.dex import DEX

with open('extracted_apk/classes4.dex', 'rb') as f:
    d = DEX(f.read())
    for c in d.get_classes():
        if 'MainActivityKt' == c.get_name().split('/')[-1][:-1]:
            for m in c.get_methods():
                if m.get_name() == 'CalculatorApp':
                    print("=== METHOD: CalculatorApp ===")
                    code = m.get_code()
                    if code:
                        for ins in code.get_bc().get_instructions():
                            print(f"{ins.get_name():20} {ins.get_output()}")
