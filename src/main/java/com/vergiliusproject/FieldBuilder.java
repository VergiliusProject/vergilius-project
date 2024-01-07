package com.vergiliusproject;

import com.vergiliusproject.repos.TtypeRepository;
import java.util.*;
import java.util.stream.Collectors;

public class FieldBuilder {
    private String name = new String();
    private final StringBuilder type = new StringBuilder();
    private StringBuilder dim = new StringBuilder();
    private final StringBuilder retval = new StringBuilder();
    private final StringBuilder args = new StringBuilder();
    private int fbOffset = 0;

    //Method returns a string indent for a type, which depends on nesting level of this type
    public static String retIndent(int indent) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            //4 spaces
            str.append("    ");
        }
        
        return str.toString();
    }

    public static String retSpaces(int length) {
        StringBuilder outputBuffer = new StringBuilder();
        if (length <= 75) {
            for (int i = 0; i <= 75 - length; i++) {
                outputBuffer.append(" ");
            }
            
            return outputBuffer.toString() + "//0x";
        } else {
            return " //0x";
        }
    }

    @Override
    public String toString() {
        if (retval.toString().isEmpty()) {
            return type + " " + name + dim;
        }
        
        return retval + "(" + type + name + dim + ")" + "(" + args + ")";
    }

    public static boolean isTopLevel(int indent) {
        return indent == 0;
    }

    public static void printEnumFields(FieldBuilder fb, Ttype type, int indent, Os operSys) {
        //if not bodyless
        if (type.getData() != null && type.getSizeof() != 0) {
            fb.type.append("\n").append(retIndent(indent)).append("{");

            List<Tdata> enumData = Sorter.sortByOrdinal(type.getData());
            indent++;

            for (int i = 0; i < enumData.size() - 1; i++) {
                fb.type.append("\n").append(retIndent(indent)).append(enumData.get(i).getName()).append(" = ").append(enumData.get(i).getOffset()).append(",");
            }

            fb.type.append("\n").append(retIndent(indent)).append(enumData.get(enumData.size() - 1).getName()).append(" = ").append(enumData.get(enumData.size() - 1).getOffset());

            fb.type.append(retIndent(--indent)).append("\n}");
        }
    }

    public static boolean isBitField(String fieldName) {
        return fieldName.contains(":");
    }

    public static List<Integer> findDuplicates(List<Tdata> list, int val) {
        List<Integer> dupl = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getOffset() == val) {
                dupl.add(i);

                if (isBitField(list.get(i).getName())) {
                    while (i + 1 < list.size() && isBitField(list.get(i + 1).getName())) {
                        i++;
                    }
                }
            }

        }
        
        if (dupl.size() <= 1) {
            dupl.clear();
        }
        
        return dupl;
    }

    public static int getSizeOfUnion(List<Tdata> structFields, TtypeRepository repo, Os operSys, List<Integer> dupls) {
        int[] sizes = new int[dupls.size() - 1];
        int k = 0;

        for (int i = dupls.size() - 1; i > 0; i--) {
            //prev
            sizes[k] = repo.findByIdAndOpersys(structFields.get(dupls.get(i) - 1).getId(), operSys).getSizeof() + structFields.get(dupls.get(i) - 1).getOffset();
            k++;
        }
        Arrays.sort(sizes);
        return sizes[sizes.length - 1];
    }

    public static List<List<Tdata>> getFirstPieceOfStruct(List<Tdata> structFields, TtypeRepository repo, Os operSys) {
        List<List<Tdata>> retLists = new ArrayList<>();
        Tdata currentField = structFields.get(0);

        List<Integer> dupl = findDuplicates(structFields, currentField.getOffset());
        if (!dupl.isEmpty()) {
            int sizeOfUnion = getSizeOfUnion(structFields, repo, operSys, dupl);
            int maxPossibleOffset = sizeOfUnion - 1;

            for (int j = 0; j < dupl.size() - 1; j++) {
                List<Tdata> list1 = new ArrayList<>();
                for (int n = dupl.get(j); n < dupl.get(j + 1); n++) {
                    list1.add(structFields.get(n));
                }
                retLists.add(list1);
            }

            int bottomBorder = dupl.get(dupl.size() - 1);
            for (int k = bottomBorder + 1; k < structFields.size(); k++) {
                if (structFields.get(k).getOffset() > maxPossibleOffset) {
                    bottomBorder = k - 1;
                    break;
                } else {
                    bottomBorder++;
                }
            }

            List<Tdata> list1 = new ArrayList<>();
            for (int n = dupl.get(dupl.size() - 1); n < bottomBorder + 1; n++) {
                list1.add(structFields.get(n));
            }

            retLists.add(list1);
        } else {
            List<Tdata> single = new ArrayList<>();
            single.add(structFields.get(0));
            retLists.add(single);
        }

        for (int m = 0; m < retLists.size(); m++) {
            for (Tdata each : retLists.get(m)) {
                structFields.remove(each);
            }
        }

        return retLists;
    }

    public static void constructFieldType(Tdata currentField, FieldBuilder fb, Ttype type, int rpOffset, String link, TtypeRepository repo, Os operSys, int indent) {
        type = repo.findByIdAndOpersys(currentField.getId(), operSys);

        FieldBuilder field = FieldBuilder.recursionProcessing(repo, type, indent, rpOffset + currentField.getOffset(), link, operSys);
        field.setName(currentField.getName());
        field.fbOffset = rpOffset + currentField.getOffset();

        fb.type.append("\n").append(retIndent(indent)).append(field.toString()).append(";"); //string creation

        String str = new StringBuilder("\n").append(retIndent(indent)).append(field.toString()).append(";").toString();
        String[] splitted = str.replaceAll("(<.+?>)", "").split("\n");

        int displayLength = splitted[splitted.length - 1].length();
        fb.type.append(retSpaces(displayLength)).append(Integer.toHexString(field.fbOffset));
    }

    public static void recStructProcessing(List<Tdata> structFields, FieldBuilder fb, Ttype type, int rpOffset, String link, TtypeRepository repo, Os operSys, int indent) {
        while (!structFields.isEmpty()) {
            List<List<Tdata>> returned = getFirstPieceOfStruct(structFields, repo, operSys);
            
            if (returned.size() > 1) {
                fb.type.append("\n").append(retIndent(indent)).append("union").append("\n").append(retIndent(indent)).append("{");
                indent++;
                
                for (List<Tdata> each: returned) {
                    if (each.size() > 1) {
                        fb.type.append("\n").append(retIndent(indent)).append("struct").append("\n").append(retIndent(indent)).append("{");
                        indent++;
                        recStructProcessing(each, fb, type, rpOffset, link, repo, operSys, indent);
                        indent--;
                        fb.type.append("\n").append(retIndent(indent)).append("};");
                    } else {
                        Tdata currentField = each.get(0);
                        constructFieldType(currentField, fb, type, rpOffset, link, repo, operSys, indent);
                    }
                }
                indent--;
                fb.type.append("\n").append(retIndent(indent)).append("};");
            } else {
                Tdata currentField = returned.get(0).get(0);
                constructFieldType(currentField, fb, type, rpOffset, link, repo, operSys, indent);
            }
        }
    }

    public static void printStructFields(FieldBuilder fb, Ttype type, TtypeRepository repo, int indent, int rpOffset, String link, Os operSys) {
        //if structure isn't bodyless
        if (type.getData() != null && type.getSizeof() != 0) {
            fb.type.append("\n").append(retIndent(indent)).append("{");

            //all structure fields have a deeper level of nesting
            indent++;

            List<Tdata> structFields = Sorter.sortByOrdinal(type.getData());

            recStructProcessing(structFields, fb, type, rpOffset, link, repo, operSys, indent);

            fb.type.append("\n").append(retIndent(--indent)).append("}");// braces are always on the same level with a word 'struct'
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    //The method checks type modifier (const or volatile)
    public static String getModifier(Ttype typeOfField) {
        return (typeOfField.isIsConst() ? "const" : "").isEmpty()?(typeOfField.isIsVolatile() ? "volatile" : ""): (typeOfField.isIsVolatile() ? "volatile" : "");
    }

    public static void printUnionFields(FieldBuilder fb, Ttype type, TtypeRepository repo, int indent, int rpOffset, String link, Os operSys) {
        if (type.getData() != null && type.getSizeof() != 0) {

            fb.type.append("\n").append(retIndent(indent)).append("{");

            List<Tdata> fields = Sorter.sortByOrdinal(type.getData());

            indent++;
            //"beginning" means the beginning of a nested structure
            boolean beginning = false;

            int last = fields.size() - 1;
            for (int i = 0; i < fields.size(); i++) {
                type = repo.findByIdAndOpersys(fields.get(i).getId(), operSys);

                FieldBuilder field = FieldBuilder.recursionProcessing(repo, type, indent, rpOffset + fields.get(i).getOffset(), link, operSys);
                field.setName(fields.get(i).getName());
                field.fbOffset = rpOffset + fields.get(i).getOffset();

                if (i == last) {
                    //on last iteration we close braces for nested structure (if there was such structure before this iteration)
                    if (beginning) {
                        fb.type.append("\n").append(retIndent(indent)).append("};");
                    }
                    //processing of the last iteration
                    // in union struct{...} s;

                    String str = new StringBuilder("\n").append(retIndent(indent)).append(field.toString()).append(";").toString();
                    String[] splitted = str.replaceAll("(<.+?>)", "").split("\n");
                    int displayLength = splitted[splitted.length - 1].length();
                    fb.type.append(str).append(retSpaces(displayLength)).append(Integer.toHexString(field.fbOffset));

                    break; //comparing 'fields.size()-1' and 'fields.size()' iteration leads to exception
                }

                //a same offset between two fields means that they are the fields of the same union or the same structure
                if (fields.get(i).getOffset() == fields.get(i + 1).getOffset()) {
                    String str = new StringBuilder("\n").append(retIndent(indent)).append(field.toString()).append(";").toString();
                    String[] splitted = str.replaceAll("(<.+?>)", "").split("\n");
                    int displayLength = splitted[splitted.length - 1].length();
                    fb.type.append(str).append(retSpaces(displayLength)).append(Integer.toHexString(field.fbOffset));
                }

                //a different offset and the fields aren't inside of structure
                if (fields.get(i).getOffset() != fields.get(i + 1).getOffset() && !beginning) {
                    //'opening' a structure
                    beginning = true;

                    //processing of current iteration

                    String str = new StringBuilder("\n" + retIndent(indent)).append("struct\n" + retIndent(indent) + "{\n" + retIndent(indent + 1)).append(field.toString()).append(";").toString();
                    String[] splitted = str.replaceAll("(<.+?>)", "").split("\n");

                    int displayLength = splitted[splitted.length - 1].length();
                    fb.type.append(str).append(retSpaces(displayLength)).append(Integer.toHexString(field.fbOffset));
                } else if(fields.get(i).getOffset() != fields.get(i + 1).getOffset() && beginning) {
                    //a different offset and previous field was inside of structure ->
                    //processing a current iteration and 'closing' the structure
                    beginning = false;

                    String str = new StringBuilder("\n").append(retIndent(indent + 1)).append(field.toString()).append(";").toString();
                    String[] splitted = str.replaceAll("(<.+?>)", "").split("\n");

                    int displayLength = splitted[splitted.length - 1].length();
                    fb.type.append(str).append(retSpaces(displayLength)).append(Integer.toHexString(field.fbOffset));

                    fb.type.append("\n").append(retIndent(indent)).append("};");
                }
            }
            
            indent--;
            fb.type.append("\n").append(retIndent(indent)).append("}");
        }
    }

    //Method returns an object(fb), which represents some type.
    public static FieldBuilder recursionProcessing(TtypeRepository repo, Ttype type, int indent, int rpOffset, String link, Os operSys) {
        switch (type.getKind()) {
            case BASE -> {
                FieldBuilder fb = new FieldBuilder();
                fb.type.append(getModifier(type).isEmpty()? type.getName():getModifier(type) + " " + type.getName()); //type.getName() -> int, char etc.

                return fb;
            }
            
            case POINTER -> {
                //getting a new type on which "current" type points to
                Tdata refType = type.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());

                String name = repo.findByIdAndOpersys(refType.getId(), operSys).getName();

                if (repo.findByIdAndOpersys(refType.getId(), operSys).getKind() == Ttype.Kind.STRUCT && name.equals("<unnamed-tag>")) {
                    FieldBuilder fb = FieldBuilder.recursionProcessing(repo, repo.findByIdAndOpersys(refType.getId(), operSys), indent, rpOffset, link, operSys);
                    fb.type.append("*").append(getModifier(type).isEmpty() ? "" : (" " + getModifier(type)));
                    fb.fbOffset = rpOffset;
                    return fb;
                }

                if (repo.findByIdAndOpersys(refType.getId(), operSys).getKind() == Ttype.Kind.STRUCT) {
                    FieldBuilder fb = new FieldBuilder();

                    fb.type.append("struct<a class='str-link' tabindex='-1' href='").append(link).append(name).append("'> ").append(name).append("</a>*").append(getModifier(type).isEmpty() ? "" : (" " + getModifier(type)));
                    return fb;
                }

                FieldBuilder fb = recursionProcessing(repo, repo.findByIdAndOpersys(refType.getId(), operSys), indent, rpOffset, link, operSys);
                fb.type.append("*").append(getModifier(type).isEmpty() ? "" : (" " + getModifier(type)));
                return fb;
            }
            
            case ARRAY -> {
                Tdata refType = type.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());
                type = repo.findByIdAndOpersys(refType.getId(), operSys);
                FieldBuilder fb = recursionProcessing(repo, type, indent, rpOffset, link, operSys);
                fb.dim = new StringBuilder("[" + refType.getOffset() + "]" + fb.dim);
                return fb;
            }
            
            case FUNCTION -> {
                FieldBuilder fb = new FieldBuilder();

                List<Tdata> funcComponents = Sorter.sortByOrdinal(type.getData().stream().collect(Collectors.toSet()));

                int counter = 0;
                for (Tdata component : funcComponents) {
                    Ttype typeOfComponent = repo.findByIdAndOpersys(component.getId(), operSys);
                    FieldBuilder fbType = recursionProcessing(repo, typeOfComponent, indent, rpOffset, link, operSys);
                    if ("return".equals(component.getName())) {
                        fb.retval.append(fbType.toString());
                    } else {
                        fbType.name = "arg" + counter;
                        fb.args.append(fbType.toString());
                        if (counter != (funcComponents.size() - 1)) {
                            fb.args.append(", ");
                        }
                    }
                    counter++;
                }
                
                return fb;
            }
            
            case STRUCT -> {
                FieldBuilder fb = new FieldBuilder();

                if (isTopLevel(indent)) {
                    //size of structure in hex format
                    fb.type.append("//0x").append(Integer.toHexString(type.getSizeof())).append(" bytes (sizeof)\n");

                    fb.type.append("struct ").append(type.getName());

                    printStructFields(fb, type, repo, indent, rpOffset, link, operSys);
                    fb.type.append(";");
                } else {   
                    //not top-level structure without name should be displayed with it's all fields
                    //directly at that place, where it's declared
                    if (type.getName().equals("<unnamed-tag>") || type.getName().equals("__unnamed")) {
                        fb.type.append("struct");
                        printStructFields(fb, type, repo, indent, rpOffset, link, operSys);
                    } else {
                        //not top-level structures should be displayed with links
                        fb.type.append(getModifier(type).isEmpty()? "" : (getModifier(type) + " ")).append("struct <a class='str-link' tabindex='-1' href='").append(link).append(type.getName()).append("'>").append(type.getName()).append("</a>");
                    }
                }
                return fb;
            }
            
            case ENUM -> {
                FieldBuilder fb = new FieldBuilder();

                //top-level enum(structure, union) always have a name
                if (isTopLevel(indent)) {
                    //size (hex)
                    fb.type.append("//0x").append(Integer.toHexString(type.getSizeof())).append(" bytes (sizeof)\n");
                    fb.type.append(new StringBuilder("enum " + type.getName()));
                    printEnumFields(fb, type, indent, operSys);
                    fb.type.append(";");
                } else {
                    if (type.getName().equals("<unnamed-tag>") || type.getName().equals("__unnamed")) {
                        fb.type.append(new StringBuilder("enum"));
                        printEnumFields(fb, type, indent, operSys);
                    } else {
                        fb.type.append("enum").append(getModifier(type).isEmpty()? "" : (getModifier(type))).append("<a class='str-link' tabindex='-1' href='").append(link).append(type.getName()).append("'> ").append(type.getName()).append("</a>");
                    }
                }
                return fb;
            }
            
            case UNION -> {
                FieldBuilder fb = new FieldBuilder();
                if (isTopLevel(indent)) {
                    //size (hex)
                    fb.type.append("//0x").append(Integer.toHexString(type.getSizeof())).append(" bytes (sizeof)\n");

                    fb.type.append(new StringBuilder("union " +  type.getName()));

                    printUnionFields(fb, type, repo, indent, rpOffset, link, operSys);

                    fb.type.append(";");
                } else {
                    if (type.getName().equals("<unnamed-tag>") || type.getName().equals("__unnamed")) {
                        fb.type.append(getModifier(type)).append("union");

                        printUnionFields(fb, type, repo, indent, rpOffset, link, operSys);
                    } else {
                        fb.type.append("union").append(getModifier(type).isEmpty()? "" : (getModifier(type))).append("<a class='str-link' tabindex='-1' href='").append(link).append(type.getName()).append("'> ").append(type.getName()).append("</a>");
                    }
                }
                return fb;
            }
            
            default -> {
                return new FieldBuilder();
            }
        }
    }
}