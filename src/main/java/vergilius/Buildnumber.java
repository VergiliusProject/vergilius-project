package vergilius;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Buildnumber {

    public static int compareBuildnumbers(String buildnumber1, String buildnumber2) {
        List<Integer> bn1Parts = Arrays.stream(buildnumber1.split("\\.")).map(Integer::parseInt).collect(Collectors.toList());
        List<Integer> bn2Parts = Arrays.stream(buildnumber2.split("\\.")).map(Integer::parseInt).collect(Collectors.toList());

    return new CompareToBuilder()
                    .append(bn1Parts.get(0), bn2Parts.get(0))
                    .append(bn1Parts.get(1), bn2Parts.get(1))
                    .append(bn1Parts.get(2), bn2Parts.get(2))
                    .append(bn1Parts.get(3), bn2Parts.get(3))
                    .toComparison();
    }
}
