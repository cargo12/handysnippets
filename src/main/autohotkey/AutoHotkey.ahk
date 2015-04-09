; IMPORTANT INFO ABOUT GETTING STARTED: Lines that start with a
; semicolon, such as this one, are comments.  They are not executed.

; This script has a special filename and path because it is automatically
; launched when you run the program directly.  Also, any text file whose
; name ends in .ahk is associated with the program, which means that it
; can be launched simply by double-clicking it.  You can have as many .ahk
; files as you want, located in any folder.  You can also run more than
; one .ahk file simultaneously and each will get its own tray icon.

; SAMPLE HOTKEYS: Below are two sample hotkeys.  The first is Win+Z and it
; launches a web site in the default browser.  The second is Control+Alt+N
; and it launches a new Notepad window (or activates an existing one).  To
; try out these hotkeys, run AutoHotkey again, which will load this file.

;#z::Run http://ahkscript.org

; Create the popup menu by adding some items to it.
Menu, VMMenu, Add, Bodhi, MenuHandlerBodhi
Menu, VMMenu, Add, Ubuntu, MenuHandlerUbuntu
return  ; End of script's auto-execute section.

MenuHandlerUbuntu:
	run "...\Ubuntu 64-bit.vmx"
return
MenuHandlerBodhi:
	run "...\Bodhi Linux 3.0.0.vmx"
return

#v::Menu, VMMenu, Show  ; i.e. press the Win-Z hotkey to show the menu.


SetTitleMatchMode, 2

; Alt-Strg-N
; # - Win key
; ^ - Strg
; ! - Alt


^!r::
Reload
	Sleep 1000 ; If successful, the reload will close this instance during the Sleep, so the line below will never be reached.
	MsgBox, 4,, The script could not be reloaded. Would you like to open it for editing?
	IfMsgBox, Yes, Edit
return

; Ctrl-Alt-X  Alternative to Alt-F4
^!x::
	Send !{F4}
return

; Numpad as debugger keys
Numpad0::
	Send {F5}
return
NumpadDot::
	Send {F6}
return
+Numpad0::
	Send {F10}
return
+NumpadDot::
	Send {F11}
return

NumpadEnter::
	Send {F8}
return
NumpadAdd::
	Send {F7}
return

^#!c::
	Run C:\cygwin\bin\mintty.exe -i /Cygwin-Terminal.ico -
return

#!c::
IfWinExist ahk_class mintty
	IfWinActive ahk_class mintty
		WinMinimize
	else
		WinActivate
else
	Run C:\cygwin\bin\mintty.exe -i /Cygwin-Terminal.ico -
return

#!e::
IfWinExist ahk_class SWT_Window0
	IfWinActive ahk_class SWT_Window0
		WinMinimize
	else
		WinActivate
else
	Run c:\Eigenes\Programme\eclipse44\eclipse.exe
return

#n::
IfWinExist ahk_class Notepad++
	IfWinActive ahk_class Notepad++
		WinMinimize
	else
		WinActivate
else
	Run C:\Eigenes\Programme\Notepad++Portable\Notepad++Portable.exe
return

^CtrlBreak::
IfWinExist ahk_class Notepad++
	IfWinActive ahk_class Notepad++
		WinMinimize
	else
		WinActivate
else
	Run C:\Eigenes\Programme\Notepad++Portable\Notepad++Portable.exe
return

Break::
IfWinExist ahk_class rctrl_renwnd32
	IfWinActive
		WinMinimize
	else 
		WinActivate
else
	Run Outlook
return


#f::
IfWinExist ahk_class MozillaWindowClass
	IfWinActive ahk_class MozillaWindowClass
		WinMinimize
	else
		WinActivate
else
	Run firefox
return

#g::
IfWinExist ahk_class Chrome_WidgetWin_1
	IfWinActive ahk_class Chrome_WidgetWin_1
		WinMinimize
	else
		WinActivate
else
	Run chrome
return

#j::
InputBox, UserInput, Jira-Nummer (ohne BUG-), Bitte Jira-Nummer eingeben, , 300, 100,,,,,%clipboard%
if !ErrorLevel
	run C:\Program Files (x86)\Google\Chrome\Application\chrome.exe "https://.../jira/browse/BUG-%UserInput%"
return

!#j::
InputBox, UserInput, Jira-Nummer (ohne BUG-), Bitte Jira-Nummer eingeben, , 300, 100,,,,,%clipboard%
if ErrorLevel
    MsgBox, CANCEL was pressed.
else
;    MsgBox, You entered "%UserInput%"
	run C:\Program Files (x86)\Google\Chrome\Application\chrome.exe "https://.../jira/browse/%UserInput%"
return

; WindowClass SunAwtFrame
#t::
;IfWinExist , ahk_class SunAwtFrame, hoppi
IfWinExist  hoppi
	IfWinActive
		WinMinimize
	else
		WinActivate
else
;	MsgBox, 4, , Do you want to continue? (Press YES or NO)
	Run ....bat.lnk
return

^!l::
IfWinExist ahk_class CommunicatorMainWindowClass
	IfWinActive
		WinMinimize
	else
		WinActivate
else
	Run lync
return

#F11::
	Send 2440{#}
return

#F12::
	Send 3459{#}
return

; Decrease sound volume
NumpadMult::
	Send {Volume_Down 10} 
return

; Increase sound volume
NumpadSub::
	Send {Volume_Up 10}
return

; Mute sound volume
NumpadDiv::
	Send {Volume_Mute}
return

; open mixer
^NumpadSub::
	Run sndvol
return

; Win-F2 open c:\Eigenes\tmp
#F2::
;Run, Explorer, "/select,%MyVar%"
	Run, Explore "c:\Eigenes\tmp"
return

; Win-F3 open c:\Eigenes\Downloads
#F3::
;Run, Explorer, "/select,%MyVar%"
	Run, Explore "c:\Eigenes\Downloads"
return

; Note: From now on whenever you run AutoHotkey directly, this script
; will be loaded.  So feel free to customize it to suit your needs.

; Please read the QUICK-START TUTORIAL near the top of the help file.
; It explains how to perform common automation tasks such as sending
; keystrokes and mouse clicks.  It also explains more about hotkeys.
