/*
 * Author:  Filip Dvorak <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvorak <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package scripting;

import fLib.utils.io.FileHandling;

/**
 *
 * @author FD
 */
public class Misc {

    public static void main(String[] args) {

        String out = "type Airport;\n"
                + "\n"
                + "type Passenger < object with {\n"
                + "  variable Airport location;  \n"
                + "};\n"
                + "\n"
                + "\n"
                + "action fly(Passenger p, Airport a, Airport b){\n"
                + "  [all] p.location == a :-> b;\n"
                + "};\n"
                + "\n"
                + "instance Passenger p1,p2,p3,p4,p5,p6,p7,p8,p9,p10;\n"
                + "\n"
                + "instance Airport prague, \n"
                + "\n"
                + "@"
                + "\n"
                + "barcelona;\n"
                + "\n"
                + "\n"
                + "[start]{"
                + "  p1.location := prague;\n"
                + "  p2.location := prague;\n"
                + "  p3.location := prague;\n"
                + "  p4.location := prague;\n"
                + "  p5.location := prague;\n"
                + "  p6.location := prague;\n"
                + "  p7.location := prague;\n"
                + "  p8.location := prague;\n"
                + "  p9.location := prague;\n"
                + "  p10.location := prague;\n"
                + "};"
                + "\n"
                + "[end] {\n"
                + "  p1.location == barcelona;\n"
                + "  p2.location == barcelona;\n"
                + "  p3.location == barcelona;\n"
                + "  p4.location == barcelona;\n"
                + "  p5.location == barcelona;\n"
                + "  p6.location == barcelona;\n"
                + "  p7.location == barcelona;\n"
                + "  p8.location == barcelona;\n"
                + "  p9.location == barcelona;\n"
                + "  p10.location == barcelona;\n"
                + "};";

        for (int n = 0; n < 10; n++) {
            String pom = "";
            for (int i = 0; i <= n * 2000; i++) {
                pom += "a" + i + ",";
                if (i % 50 == 0) {
                    pom += "\n";
                }
            }
            FileHandling.writeFileOutput("scale" + n + ".anml", out.replaceAll("@", pom));
        }

    }
}
