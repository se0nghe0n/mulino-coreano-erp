import fs from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import { spawnSync } from 'node:child_process';

// A private Fontconfig config exposes installed Korean fonts to headless Impress.
// Does not install fonts, modify runtime dependencies, or alter system settings.
const [inputArg,outputArg]=process.argv.slice(2);
if(!inputArg || !outputArg) throw new Error('Usage: node export-pdf.mjs <final.pptx> <new-output-dir>');
const input=path.resolve(inputArg),output=path.resolve(outputArg);
const pdf=path.join(output,path.basename(input).replace(/\.pptx$/i,'.pdf'));
try{await fs.access(pdf);throw new Error('Choose a new output directory; PDF exists.');}catch(e){if(e.code!=='ENOENT')throw e;}
const scratch=await fs.mkdtemp(path.join(os.tmpdir(),'mulino-pdf-'));
const escape=value=>value.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;');
const fontDirs=process.env.PORTFOLIO_FONT_DIRS?.split(path.delimiter) ?? ['/System/Library/Fonts','/System/Library/Fonts/Supplemental','/Library/Fonts'];
const config=path.join(scratch,'fonts.conf');
await fs.writeFile(config,`<?xml version="1.0"?><!DOCTYPE fontconfig SYSTEM "fonts.dtd"><fontconfig>${fontDirs.map(d=>`<dir>${escape(d)}</dir>`).join('')}<cachedir>${escape(path.join(scratch,'font-cache'))}</cachedir></fontconfig>`);
await fs.mkdir(output,{recursive:true});
try{
 const result=spawnSync(process.env.SOFFICE ?? 'soffice',['--headless','--convert-to','pdf','--outdir',output,input],{env:{...process.env,FONTCONFIG_FILE:config},encoding:'utf8'});
 if(result.status!==0)throw new Error(result.stderr || result.error?.message || 'PDF export failed.');
 await fs.access(pdf);console.log(JSON.stringify({pdf,exporter:'headless Impress',fontDirs}));
}finally{await fs.rm(scratch,{recursive:true,force:true});}
