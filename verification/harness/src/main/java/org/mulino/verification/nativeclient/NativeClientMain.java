package org.mulino.verification.nativeclient;

import org.mulino.verification.Json;

/** Non-model probe only. Actual runs must enter through the gated acceptance runner. */
public final class NativeClientMain {
    public static void main(String[] args)throws Exception {
        if(args.length!=1||!args[0].equals("preflight"))throw new IllegalArgumentException("preflight only; no direct model-call bypass");
        try{System.out.println(Json.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(new NativeCodexClient().preflight()));}
        catch(Exception e){String code=e.getMessage();if(code==null||!code.matches("[A-Z0-9_]{1,100}"))code="NATIVE_PREFLIGHT_FAILED";
            System.out.println(Json.object().put("status","NOT_RUN").put("actualModelCalls",0).put("blocker",code));System.exit(2);}
    }
}
