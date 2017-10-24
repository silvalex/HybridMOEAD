
for i in {1..8}; do
	mkdir ~/grid/2008-hybrid$i
	for j in {1..50}; do
		result=~/grid/2008-hybrid$i/out$j.stat
		front=~/grid/2008-hybrid$i/front$j.stat
		java -cp program.jar moead.MOEAD moead.params seed $j outFileName $result frontFileName $front serviceTask /am/state-opera/home1/sawczualex/workspace/wsc2008/Set0${i}MetaData/problem.xml serviceTaxonomy /am/state-opera/home1/sawczualex/workspace/wsc2008/Set0${i}MetaData/taxonomy.xml serviceRepository /am/state-opera/home1/sawczualex/workspace/wsc2008/Set0${i}MetaData/services-output.xml
	done
done

echo "Done!"
