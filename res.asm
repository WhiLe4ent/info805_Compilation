DATA SEGMENT
	x DD
	y DD
DATA ENDS
CODE SEGMENT
	in eax
	mov x, eax
	in eax
	mov y, eax
	mov eax, z
	out eax
CODE ENDS
