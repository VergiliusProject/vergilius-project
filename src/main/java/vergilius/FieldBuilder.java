package vergilius;

import vergilius.repos.TtypeRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class FieldBuilder
{
    private String name = new String();
    private StringBuilder type = new StringBuilder();
    private StringBuilder dim = new StringBuilder();
    private StringBuilder retval = new StringBuilder();
    private StringBuilder args = new StringBuilder();
    private int fbOffset = 0;
    private int realLength = 0;

    //The method returns a string indent for a type, which depends on nesting level of this type
    public static String retIndent(int indent)
    {
        StringBuilder str = new StringBuilder();
        for(int i = 0; i < indent; i++)
        {
            //4 spaces
            str.append("    ");
        }
        return str.toString();
    }

    public static String retSpaces(int length)
    {
        StringBuilder outputBuffer = new StringBuilder();
        if(length <= 75)
        {
            for (int i = 0; i <= 75 - length; i++){
                outputBuffer.append(" ");
            }
            return outputBuffer.toString() + "//0x";
        }
        else
        {
            //8 spaces
            return "        " + "//0x";
        }
    }

    public String toString()
    {
        if(retval.toString().isEmpty())
        {
            return type + " " + name + dim;
        }
        return retval + "(" + type + name + dim + ")" + "(" + args + ")";
    }

    public static boolean isTopLevel(int indent)
    {
        return indent == 0;
    }

    public static void printStructFields(FieldBuilder fb, Ttype type, TtypeRepository repo, int indent, int rpOffset, String link)
    {
        //if structure isn't bodiless
        if(type.getData() != null && type.getSizeof() != 0)
        {
            fb.type.append("\n").append(retIndent(indent)).append("{");

            indent++; //all structure fields have a deeper level of nesting
            List<Tdata> structFields = Sorter.sortByOrdinal(type.getData());

            for (Tdata currentField : structFields)
            {
                type = repo.findOne(currentField.getId());

                FieldBuilder field = FieldBuilder.recursionProcessing(repo, type, indent, rpOffset + currentField.getOffset(), link);
                field.setName(currentField.getName());
                field.fbOffset = rpOffset + currentField.getOffset();

                fb.type.append("\n").append(retIndent(indent)).append(field.toString()).append(";"); //string creation

                //In cases when field.type includes html-link, we calculate the realLength in different way then the field.type doesn't include it
                //If realLength == 0, it means that it haven't been calculated in some special way yet and a simple approach in else {} should be used,
                //otherwise we calculate realLength without a length of html-link tags

                if(field.realLength != 0)
                {
                    //length calculation and adding a comment with hex offset-value
                    //calculation of spaces for functions in else {...}
                    fb.type.append(retSpaces(retIndent(indent).length() + field.realLength + " ".length() + field.name.length() + field.dim.length() + ";".length()) + Integer.toHexString(field.fbOffset));
                }
                else
                {
                    //if it's ptr on func declaration with link
                    if(field.args.toString().contains("<"))
                    {
                        String forCut = field.args.toString();
                        String linkLength = forCut.substring(forCut.indexOf("<"),forCut.indexOf(">") + 1) + forCut.substring(forCut.lastIndexOf("<"), forCut.lastIndexOf(">") +1);
                        //System.out.print(linkLength);
                        fb.type.append(retSpaces(retIndent(indent).length() + (field.toString().length() - linkLength.length() + ";".length())) + Integer.toHexString(field.fbOffset));
                    }
                    else
                    {
                        //for ptr on func without link and for all other declarations
                        fb.type.append(retSpaces(retIndent(indent).length() + field.toString().length() + ";".length()) + Integer.toHexString(field.fbOffset));
                    }

                }
            }
            fb.type.append("\n").append(retIndent(--indent)).append("}");// braces are always on the same level with a word 'struct'
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    //The method checks type's modifier (const or volatile)
    public static String getModifier(Ttype typeOfField)
    {
        return (typeOfField.isIsConst() ? "const" : "").isEmpty()?(typeOfField.isIsVolatile() ? "volatile" : ""): (typeOfField.isIsVolatile() ? " volatile" : "");
    }

    public static void printUnionFields(FieldBuilder fb, Ttype type, TtypeRepository repo, int indent, int rpOffset, String link)
    {
        List<Tdata> fields = Sorter.sortByOrdinal(type.getData());

        indent++;
        //"beginning" means the beginning of a nested structure
        boolean beginning = false;

        int last = fields.size() - 1;
        for(int i = 0; i < fields.size(); i++)
        {
            type = repo.findOne(fields.get(i).getId());

            FieldBuilder field = FieldBuilder.recursionProcessing(repo, type, indent, rpOffset + fields.get(i).getOffset(), link);
            field.setName(fields.get(i).getName());
            field.fbOffset = rpOffset + fields.get(i).getOffset();

            if(i == last)
            {
                //on last iteration we close braces for nested structure (if there was such structure before this iteration)
                if(beginning)
                {
                    fb.type.append("\n").append(retIndent(indent)).append("};");
                }
                //processing of the last iteration
                // in union struct{...} s;
                if(field.realLength != 0)
                {
                    fb.type.append("\n" + retIndent(indent) + field.toString() + ";" + retSpaces(retIndent(indent).length() + + field.realLength + " ".length() + field.name.length()  +";".length()) + Integer.toHexString(field.fbOffset));
                }
                else
                {
                    fb.type.append("\n" + retIndent(indent) + field.toString() + ";" + retSpaces(retIndent(indent).length() + field.toString().length()  +";".length()) + Integer.toHexString(field.fbOffset));
                }

                break; //comparing 'fields.size()-1' and 'fields.size()' iteration will lead to exception
            }

            //a same offset between two fields means that they are the fields of the same union or the same structure
            if(fields.get(i).getOffset() == fields.get(i + 1).getOffset())
            {
                if(field.realLength != 0)
                {
                    //for unnamed struct
                    fb.type.append("\n" + retIndent(indent) + field.toString() + ";" + retSpaces(retIndent(indent).length() + field.realLength + " ".length() + field.name.length()  + ";".length()) + Integer.toHexString(field.fbOffset));
                }
                else
                {
                    fb.type.append("\n" + retIndent(indent) + field.toString() + ";" + retSpaces(retIndent(indent).length() + field.toString().length() + ";".length()) + Integer.toHexString(field.fbOffset));
                }
            }

            //a different offset and the fields aren't inside of structure
            if(fields.get(i).getOffset() != fields.get(i + 1).getOffset() && !beginning)
            {
                //'opening' a structure
                beginning = true;

                //processing of current iteration
                fb.type.append("\n").append(retIndent(indent)).append("struct\n" + retIndent(indent) + "{\n" + retIndent(indent + 1) + field.toString() + ";" + retSpaces(retIndent(indent + 1).length() + field.toString().length() + ";".length()) + Integer.toHexString(field.fbOffset));
            }
            else if(fields.get(i).getOffset() != fields.get(i + 1).getOffset() && beginning)
            {
                //a different offset and previous field was inside of structure ->
                //processing a current iteration and 'closing' the structure
                beginning = false;

                fb.type.append("\n").append(retIndent(indent + 1)).append(field.toString() + ";" + retSpaces(retIndent(indent + 1).length() + field.toString().length() + ";".length())  + Integer.toHexString(field.fbOffset));
                fb.type.append("\n").append(retIndent(indent)).append("};");
            }
        }
        indent--;

    }

    //The method returns an object(fb), which represents some type.
    public static FieldBuilder recursionProcessing(TtypeRepository repo, Ttype type, int indent, int rpOffset, String link)
    {
        switch (type.getKind())
        {
            case BASE:
            {
                FieldBuilder fb = new FieldBuilder();
                fb.type.append(getModifier(type).isEmpty()? type.getName():getModifier(type) + " " + type.getName()); //type.getName() -> int, char etc.

                return fb;
            }
            case POINTER:
            {
                //getting a new type on which "current" type points to
                Tdata refType = type.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());

                String name = repo.findOne(refType.getId()).getName();

                if(repo.findOne(refType.getId()).getKind() == Ttype.Kind.STRUCT && name.equals("<unnamed-tag>"))
                {
                    FieldBuilder fb = FieldBuilder.recursionProcessing(repo, repo.findOne(refType.getId()), indent, rpOffset, link);
                    fb.type.append("*" + (getModifier(type).isEmpty() ? "" : (" " + getModifier(type))));
                    fb.fbOffset = rpOffset;
                    return fb;
                }

                //union, enum... ???
                if(repo.findOne(refType.getId()).getKind() == Ttype.Kind.STRUCT)
                {
                    FieldBuilder fb = new FieldBuilder();

                    fb.type.append("struct" + "<a class='str-link' tabindex='-1' href='" + link + name + "'> " + name + "</a>" + "*" + (getModifier(type).isEmpty() ? "" : (" " + getModifier(type))));
                    fb.realLength = ("struct" + name + "*" + (getModifier(type).isEmpty() ? "" : (" " + getModifier(type)))).length() + 1;
                    return fb;
                }

                //losing realLength
                FieldBuilder fb = recursionProcessing(repo, repo.findOne(refType.getId()), indent, rpOffset, link);
                fb.type.append("*" + (getModifier(type).isEmpty() ? "" : (" " + getModifier(type))));
                return fb;

            }
            case ARRAY:
            {
                Tdata refType = type.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());
                type = repo.findOne(refType.getId());
                FieldBuilder fb = recursionProcessing(repo, type, indent, rpOffset, link);
                fb.dim = new StringBuilder("[" + refType.getOffset() + "]" + fb.dim);
                return fb;
            }
            case FUNCTION:
            {
                FieldBuilder fb = new FieldBuilder();

                List<Tdata> funcComponents = Sorter.sortByOrdinal(type.getData().stream().collect(Collectors.toSet()));

                int counter = 0;
                for (Tdata component : funcComponents)
                {
                    Ttype typeOfComponent = repo.findOne(component.getId());
                    FieldBuilder fbType = recursionProcessing(repo, typeOfComponent, indent, rpOffset, link);
                    if ("return".equals(component.getName()))
                    {
                        fb.retval.append(fbType.toString());
                    }
                    else
                    {
                        fbType.name = "arg" + counter;
                        fb.args.append(fbType.toString());
                        if(counter != (funcComponents.size() - 1))
                        {
                            fb.args.append(", ");
                        }
                    }
                    counter++;
                }
                return fb;
            }
            case STRUCT:
            {
                FieldBuilder fb = new FieldBuilder();

                if(isTopLevel(indent))
                {
                    //hex SIZE
                    fb.type.append("//0x" + Integer.toHexString(type.getSizeof()) + " bytes (sizeof)\n");

                    fb.type.append("struct" + (getModifier(type).isEmpty()? "" : (" " + getModifier(type)))).append(type.getName().equals("<unnamed-tag>") ? "" : (" " +  type.getName()));
                    printStructFields(fb, type, repo, indent, rpOffset, link);
                    fb.type.append(";");
                }
                else
                {
                    fb.type.append("struct" + (getModifier(type).isEmpty()? "" : (" " + getModifier(type)))).append(type.getName().equals("<unnamed-tag>") ? "" : (" <a class='str-link' tabindex='-1' href='" + link + type.getName() +"'>" + type.getName()) + "</a>");
                    printStructFields(fb, type, repo, indent, rpOffset, link);
                    fb.realLength = ("}").length(); //space will be added in toString()
                }
                return fb;
            }
            case ENUM:
            {
                FieldBuilder fb = new FieldBuilder();
                if(isTopLevel(indent))
                {
                    //for enums there's no need to use recursive method (toString()!)
                    List<Tdata> enumData = Sorter.sortByOrdinal(type.getData());

                    //hex SIZE
                    fb.type.append("//0x" + Integer.toHexString(type.getSizeof()) + " bytes (sizeof)\n");

                    fb.type.append(new StringBuilder().append((type.getName() != null)? "enum " + type.getName() + "\n{\n" : "enum\n{\n"));

                    indent++;

                    for(int i = 0; i < enumData.size() - 1; i++)
                    {
                        fb.type.append(retIndent(indent) + enumData.get(i).getName() + " = " + enumData.get(i).getOffset() + "," + retSpaces((enumData.get(i).getName() + " = " + enumData.get(i).getOffset()).length() + 1) + Integer.toHexString(enumData.get(i).getOffset()) +"\n");
                    }

                    //"+ 1" means adding the length of ","
                    fb.type.append(retIndent(indent) + enumData.get(enumData.size() - 1).getName() + " = " + enumData.get(enumData.size() - 1).getOffset() + retSpaces((enumData.get(enumData.size() - 1).getName() + " = " + enumData.get(enumData.size() - 1).getOffset()).length())+ Integer.toHexString(enumData.get(enumData.size() - 1).getOffset()));
                    fb.type.append(retIndent(--indent) + "\n};");
                }
                else
                {
                    fb.type.append("enum" + (getModifier(type).isEmpty()? "" : (getModifier(type)))).append("<a class='str-link' tabindex='-1' href='" + link + type.getName() + "'> " + type.getName() + "</a>");
                    fb.realLength = ("enum" + (getModifier(type).isEmpty()? "" : (getModifier(type))).length() + type.getName()).length();
                }
                return fb;
            }
            case UNION:
            {
                FieldBuilder fb = new FieldBuilder();
                if(isTopLevel(indent))
                {
                    //hex SIZE
                    fb.type.append("//0x" + Integer.toHexString(type.getSizeof()) + " bytes (sizeof)\n");

                    fb.type.append(new StringBuilder((type.getName() != null)? ("union " +  type.getName() + "\n{") : "union\n{"));

                    //isn't bodiless check - ADD!!!

                    printUnionFields(fb, type, repo, indent, rpOffset, link);

                    fb.type.append(retIndent(indent) + "\n};");
                }
                else
                {
                    if(type.getName().equals("<unnamed-tag>"))
                    {
                        fb.type.append("union " + getModifier(type));

                        if(type.getData() != null)
                        {
                            fb.type.append("\n").append(retIndent(indent)).append("{");

                            printUnionFields(fb, type, repo, indent, rpOffset, link);

                            fb.type.append("\n").append(retIndent(indent)).append("}");

                            //union {...} u;
                            fb.realLength ="}".length();
                        }
                    }
                    else
                    {
                        fb.type.append("union" + (getModifier(type).isEmpty()? "" : (getModifier(type)))).append("<a class='str-link' tabindex='-1' href='" + link + type.getName() + "'> " + type.getName() + "</a>");
                        fb.realLength = ("union" + (getModifier(type).isEmpty()? "" : (getModifier(type))).length() + type.getName()).length();
                    }
                }
                return fb;
            }
            default:
            {
                return new FieldBuilder();
            }
        }

    }
}
