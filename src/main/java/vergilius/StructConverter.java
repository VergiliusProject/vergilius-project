package vergilius;

import vergilius.repos.TtypeRepository;
import java.util.List;


public class StructConverter {

    public static String converts(Ttype myStruct, TtypeRepository rep2, String keyWord)
    {
        List<Tdata> tmpData = Sorter.sortByOrdinal(myStruct.getData()); //fields of myStruct

            StringBuilder result = new StringBuilder(keyWord + myStruct.getName() + "\n{");
            for (int i = 0; i < tmpData.size(); i++) {
                Ttype tempType = rep2.findOne(tmpData.get(i).getId()); //type of each field

                //for "base" and "base" in pointer
                String constMod = tempType.isIsConst() ? "const " : "";
                String volatileMod = tempType.isIsVolatile() ? "volatile " : "";

                if (tempType.getKind() == Ttype.Kind.BASE)
                {
                    result.append("\n" + "\t" + constMod + volatileMod + tempType.getName() + " " + tmpData.get(i).getName() + ";");
                } else if (tempType.getKind() == Ttype.Kind.POINTER)
                {
                    Tdata[] localData = new Tdata[1];
                    localData[0] = tempType.getData().iterator().next();
                    Ttype localType = rep2.findOne(localData[0].getId());
                    if (localType.getKind() == Ttype.Kind.BASE)
                    {
                        String locCMod = localType.isIsConst() ? "const " : "";
                        result.append("\n" + "\t" + locCMod + volatileMod + localType.getName() + "*" + " " + constMod + tmpData.get(i).getName() + ";");
                    }
                    if (localType.getKind() == Ttype.Kind.FUNCTION) {
                        //pointer on function
                        String funcName = "(*" + tmpData.get(i).getName() + ")(";
                        String returnedVal = "";
                        String funcArgs = "";

                        List<Tdata> funcData = Sorter.sortByOrdinal(localType.getData());

                        for (int k = 0; k < funcData.size(); k++) {
                            if (funcData.get(k).getName() != null && funcData.get(k).getName().equals("return")) {
                                localType = rep2.findOne(funcData.get(k).getId()); //type of returned value
                                returnedVal += localType.getName(); //name of type
                            } else {
                                localType = rep2.findOne(funcData.get(k).getId()); //type of value
                                if (k < funcData.size() - 1) funcArgs += (localType.getName() + " " + "arg" + k + ", ");
                                else funcArgs += localType.getName() + " " + "arg" + k; //name of type
                            }
                        }
                        result.append("\n" + "\t" + returnedVal + " " + funcName + funcArgs + ");");
                    }
                } else if (tempType.getKind() == Ttype.Kind.ARRAY) {
                    String nameOfArray = " " + tmpData.get(i).getName();
                    String dimensions = " ";
                    String typeOfArray = "";

                    while (tempType.getKind() != Ttype.Kind.BASE) {
                        Tdata[] localData = new Tdata[1];
                        localData[0] = tempType.getData().iterator().next();

                        if (localData[0].getName().equals("element")) {
                            dimensions += "[" + localData[0].getOffset() + "]";
                        }
                        tempType = rep2.findOne(localData[0].getId());
                    }
                    if (tempType.getKind() == Ttype.Kind.BASE) {
                        typeOfArray += tempType.getName();
                    }
                    result.append("\n" + "\t" + typeOfArray + nameOfArray + dimensions + ";");
                } else if (tempType.getKind() == Ttype.Kind.ENUM) {
                    result.append("\n" + "\t" + "enum " + rep2.findOne(tmpData.get(i).getId()).getName() + " " + tmpData.get(i).getName() + ";");
                } else if (tempType.getKind() == Ttype.Kind.UNION) {
                    result.append("\n" + "\t" + "union " + rep2.findOne(tmpData.get(i).getId()).getName() + " " + tmpData.get(i).getName() + ";");
                }

            }
            result.append("\n};");
            return result.toString();
    }

}
