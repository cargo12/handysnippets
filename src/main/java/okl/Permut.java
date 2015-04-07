package okl;

/** Permutates string from argument. 
 *
 *
 * in scala:
 * scala> def perm(stump:String, rest:String):Unit={ println(stump);if(rest.size > 0) {var i:Int=0; while (i<rest.size){perm(stump+rest.charAt(i), rest.take(i) + rest.drop(i+1)); i+=1}} }
 *
 * force tail recursion check:
 * scala> import scala.annotation.tailrec
 * scala> def permOuter(s:String){@tailrec def perm(stump:String, rest:String):Unit={ println(stump);if(rest.size > 0) {var i:Int=0; while (i<rest.size){perm(stump+rest.charAt(i), rest.take(i) + rest.drop(i+1)); i+=1}} }; perm("",s)}
 * */
public class Permut {
    public static void main (String ... args){
        nimm(1, "", args[0]);
    }

    // lev : recursion level
    // stump : that part of the string that is not to be permutated (just passed unchanged into recursion)
    // rest : the part that is to be permutated
    private static void nimm (int lev, String stump, String rest){
        System.err.printf("%"+lev+"s nimm( %s  ::  %s )%n", " ", stump, rest);

        if (!"".equals(stump)) {
            System.out.printf("%s%n", stump );
        }

        if (rest.length() > 0) {
            for (int i = 0; i < rest.length(); i++){
                // left of i in rest ...
                String s = rest.substring(0, i);
                if (i < rest.length()-1) {
                    // ... right of i in rest
                    s += rest.substring(i+1, rest.length());
                }
                System.err.printf("%"+lev+"s stump='%s' char='%c' s='%s'%n", " ", stump, rest.charAt(i), s);
                nimm(lev+10, stump + rest.charAt(i), s);
            }
        }

    }
}
