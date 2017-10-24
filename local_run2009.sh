for i in {1..5}; do
	mkdir ~/grid/2009-hybrid$i
	for j in {1..50}; do

		result=~/grid/2009-hybrid$i/out$j.stat
		front=~/grid/2009-hybrid$i/front$j.stat
		java -cp program.jar moead.MOEAD moead.params seed $j outFileName $result frontFileName $front serviceTask /am/state-opera/home1/sawczualex/workspace/wsc2009/Testset0${i}/problem.xml serviceTaxonomy /am/state-opera/home1/sawczualex/workspace/wsc2009/Testset0${i}/taxonomy.xml serviceRepository /am/state-opera/home1/sawczualex/workspace/wsc2009/Testset0${i}/services-output.xml
	done
done

echo "Done!"
