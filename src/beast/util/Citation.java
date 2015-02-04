/*
 * Citation.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2015 BEAST Developers
 *
 * BEAST is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BEAST.  If not, see <http://www.gnu.org/licenses/>.
 */

package beast.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Arman Bilge
 */
@Repeatable(Citations.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Citation {

    public @interface Author {
        String surname();
        String initials();
    }

    Author[] authors();
    String title() default "";
    int year() default -1;
    String journal() default "";
    int volume() default -1;
    int startpage() default -1;
    int endpage() default -1;
    Status status() default Status.IN_PREPARATION;

    public enum Status {
        IN_PREPARATION("in preparation"),
        IN_SUBMISSION("in submission"),
        IN_PRESS("in press"),
        ACCEPTED("accepted"),
        PUBLISHED("published");

        Status(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        private final String text;
    }

    public static final class Utils {

        public static String toString(final Citation c) {
            StringBuilder builder = new StringBuilder();
            builder.append(c.authors()[0].toString());
            for (int i = 1; i < c.authors().length; i++) {
                builder.append(", ");
                builder.append(c.authors()[i].toString());
            }
            builder.append(" (");
            switch (c.status()) {
                case PUBLISHED: builder.append(c.year()); break;
                default: builder.append(c.status().getText());
            }
            builder.append(") ");
            if (!c.title().isEmpty()) {
                builder.append(c.title());
            }
            if (!c.journal().isEmpty()) {
                builder.append(". ");
                builder.append(c.journal());
            }
            if (c.status() == Status.PUBLISHED) {
                builder.append(". ");
                builder.append(c.volume());
                builder.append(", ");
                builder.append(c.startpage());
                if (c.endpage() > 0) builder.append("-").append(c.endpage());
            }
            return builder.toString();
        }

        public static String toHTML(final Citation c) {

            StringBuilder builder = new StringBuilder();
            builder.append("<html>");
            builder.append(c.authors()[0].toString());
            for (int i = 1; i < c.authors().length; i++) {
                builder.append(", ");
                builder.append(c.authors()[i].toString());
            }
            builder.append(" (").append(c.year()).append(") ");
            builder.append(c.title()).append(". ");
            builder.append("<i>").append(c.journal()).append("</i>");
            builder.append(" <b>").append(c.volume()).append("</b>:");
            builder.append(c.startpage());
            if (c.endpage() > 0) builder.append("-").append(c.endpage());
            builder.append("</html>");

            return builder.toString();
        }

        public static Citation[] getCitations(final Class<?> c) {
            if (c.isAnnotationPresent(Citations.class))
                return c.getAnnotation(Citations.class).value();
            else
                return new Citation[0];
        }

    }

}
